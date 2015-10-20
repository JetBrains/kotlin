/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.scopes

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.collectionUtils.concatInOrder
import org.jetbrains.kotlin.utils.Printer
import java.util.*

// Reads from:
// 1. Maps
// 2. Worker

// Writes to: maps

public class WritableScopeImpl @JvmOverloads constructor(
        outerScope: KtScope,
        private val ownerDeclarationDescriptor: DeclarationDescriptor,
        override val redeclarationHandler: RedeclarationHandler,
        private val debugName: String,
        implicitReceiver: ReceiverParameterDescriptor? = null,
        private val labeledDeclaration: DeclarationDescriptor? = null
) : AbstractScopeAdapter(), WritableScope, WritableScopeStorage {

    override val workerScope: KtScope = if (outerScope is WritableScope) outerScope.takeSnapshot() else outerScope

    override val addedDescriptors = SmartList<DeclarationDescriptor>()

    override var functionsByName: MutableMap<Name, WritableScopeStorage.IntList>? = null
    override var variablesAndClassifiersByName: MutableMap<Name, WritableScopeStorage.IntList>? = null

    private val implicitReceiverHierarchy = if (implicitReceiver != null)
        listOf(implicitReceiver) + super<AbstractScopeAdapter>.getImplicitReceiversHierarchy()
    else
        super<AbstractScopeAdapter>.getImplicitReceiversHierarchy()

    private var lastSnapshot: Snapshot? = null

    override fun getContainingDeclaration() = ownerDeclarationDescriptor

    private var lockLevel: WritableScope.LockLevel = WritableScope.LockLevel.WRITING

    override fun changeLockLevel(lockLevel: WritableScope.LockLevel) {
        if (lockLevel.ordinal() < this.lockLevel.ordinal()) {
            throw IllegalStateException("cannot lower lock level from " + this.lockLevel + " to " + lockLevel + " at " + toString())
        }
        this.lockLevel = lockLevel
    }

    private fun checkMayRead() {
        if (lockLevel != WritableScope.LockLevel.READING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot read with lock level " + lockLevel + " at " + toString())
        }
    }

    private fun checkMayWrite() {
        if (lockLevel != WritableScope.LockLevel.WRITING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot write with lock level " + lockLevel + " at " + toString())
        }
    }

    override fun takeSnapshot(): KtScope {
        checkMayRead()
        if (lastSnapshot == null || lastSnapshot!!.descriptorLimit != addedDescriptors.size()) {
            lastSnapshot = Snapshot(addedDescriptors.size())
        }
        return lastSnapshot!!
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        checkMayRead()
        changeLockLevel(WritableScope.LockLevel.READING)

        val result = ArrayList<DeclarationDescriptor>()
        result.addAll(addedDescriptors)
        result.addAll(workerScope.getDescriptors(kindFilter, nameFilter))
        return result
    }

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> {
        checkMayRead()

        val superResult = super<AbstractScopeAdapter>.getDeclarationsByLabel(labelName)
        return if (labeledDeclaration != null && labeledDeclaration.getName() == labelName)
            listOf(labeledDeclaration) + superResult
        else
            superResult
    }

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(variableDescriptor)
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        val descriptor = variableOrClassDescriptorByName(name)
        if (descriptor is VariableDescriptor) {
            return descriptor
        }

        return workerScope.getLocalVariable(name)
    }

    override fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()
        super<WritableScopeStorage>.addFunctionDescriptor(functionDescriptor)
    }

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        checkMayRead()

        return concatInOrder(functionsByName(name), workerScope.getFunctions(name, location))
    }

    override fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(classifierDescriptor)
    }

    override fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        checkMayRead()

        return variableOrClassDescriptorByName(name) as? ClassifierDescriptor
               ?: workerScope.getClassifier(name, location)
    }

    override fun getImplicitReceiversHierarchy() = implicitReceiverHierarchy

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = addedDescriptors

    override fun toString(): String {
        return javaClass.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + " " + debugName + " for " + getContainingDeclaration()
    }

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " for ", getContainingDeclaration(), " {")
        p.pushIndent()

        p.println("lockLevel = ", lockLevel)

        p.print("worker = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    private inner class Snapshot(val descriptorLimit: Int) : KtScope by this@WritableScopeImpl {
        override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
            checkMayRead()
            changeLockLevel(WritableScope.LockLevel.READING)

            val workerResult = workerScope.getDescriptors(kindFilter, nameFilter)

            if (descriptorLimit == 0) return workerResult

            val result = ArrayList<DeclarationDescriptor>(workerResult.size() + descriptorLimit)
            for (i in 0..descriptorLimit-1) {
                result.add(addedDescriptors[i])
            }
            result.addAll(workerResult)
            return result
        }

        override fun getLocalVariable(name: Name): VariableDescriptor? {
            checkMayRead()

            val descriptor = variableOrClassDescriptorByName(name, descriptorLimit)
            if (descriptor is VariableDescriptor) {
                return descriptor
            }

            return workerScope.getLocalVariable(name)
        }

        override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
            checkMayRead()

            return concatInOrder(functionsByName(name, descriptorLimit), workerScope.getFunctions(name, location))
        }

        override fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            checkMayRead()

            return variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor
                   ?: workerScope.getClassifier(name, location)
        }

        override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = addedDescriptors.truncated(descriptorLimit)

        private fun <T> List<T>.truncated(newSize: Int) = if (newSize == size()) this else subList(0, newSize)

        override fun printScopeStructure(p: Printer) {
            p.println(javaClass.getSimpleName(), " {")
            p.pushIndent()

            p.println("descriptorLimit = ", descriptorLimit)

            p.print("WritableScope = ")
            this@WritableScopeImpl.printScopeStructure(p.withholdIndentOnce())

            p.popIndent()
            p.println("}")
        }
    }
}
