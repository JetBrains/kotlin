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

import com.google.common.collect.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.Printer

import java.util.*
import com.intellij.util.SmartList
import org.jetbrains.kotlin.util.collectionUtils.concatInOrder

// Reads from:
// 1. Maps
// 2. Worker

// Writes to: maps

public class WritableScopeImpl(override val workerScope: JetScope,
                               private val ownerDeclarationDescriptor: DeclarationDescriptor,
                               protected val redeclarationHandler: RedeclarationHandler,
                               private val debugName: String)
: AbstractScopeAdapter(), WritableScope {

    private val explicitlyAddedDescriptors = SmartList<DeclarationDescriptor>()

    private var functionGroups: MutableMap<Name, SmartList<FunctionDescriptor>>? = null

    private var variableOrClassDescriptors: MutableMap<Name, DeclarationDescriptor>? = null

    private var labelsToDescriptors: MutableMap<Name, SmartList<DeclarationDescriptor>>? = null

    private var implicitReceiver: ReceiverParameterDescriptor? = null

    private var implicitReceiverHierarchy: List<ReceiverParameterDescriptor>? = null

    override fun getContainingDeclaration(): DeclarationDescriptor = ownerDeclarationDescriptor

    private var lockLevel: WritableScope.LockLevel = WritableScope.LockLevel.WRITING

    override fun changeLockLevel(lockLevel: WritableScope.LockLevel): WritableScope {
        if (lockLevel.ordinal() < this.lockLevel.ordinal()) {
            throw IllegalStateException("cannot lower lock level from " + this.lockLevel + " to " + lockLevel + " at " + toString())
        }
        this.lockLevel = lockLevel
        return this
    }

    protected fun checkMayRead() {
        if (lockLevel != WritableScope.LockLevel.READING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot read with lock level " + lockLevel + " at " + toString())
        }
    }

    protected fun checkMayWrite() {
        if (lockLevel != WritableScope.LockLevel.WRITING && lockLevel != WritableScope.LockLevel.BOTH) {
            throw IllegalStateException("cannot write with lock level " + lockLevel + " at " + toString())
        }
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        checkMayRead()
        changeLockLevel(WritableScope.LockLevel.READING)

        val result = ArrayList<DeclarationDescriptor>()
        result.addAll(explicitlyAddedDescriptors)
        result.addAll(workerScope.getDescriptors(kindFilter, nameFilter))
        return result
    }

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> {
        checkMayRead()

        val superResult = super<AbstractScopeAdapter>.getDeclarationsByLabel(labelName)
        val declarationDescriptors = labelsToDescriptors?.get(labelName) ?: return superResult
        if (superResult.isEmpty()) return declarationDescriptors
        return declarationDescriptors + superResult
    }

    override fun addLabeledDeclaration(descriptor: DeclarationDescriptor) {
        checkMayWrite()

        if (labelsToDescriptors == null) {
            labelsToDescriptors = HashMap()
        }
        labelsToDescriptors!!.getOrPut(descriptor.getName()) { SmartList() }.add(descriptor)
    }

    private fun addVariableOrClassDescriptor(descriptor: DeclarationDescriptor) {
        checkMayWrite()

        val name = descriptor.getName()

        val originalDescriptor = variableOrClassDescriptors?.get(name)
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, descriptor)
        }

        if (variableOrClassDescriptors == null) {
            variableOrClassDescriptors = HashMap()
        }
        variableOrClassDescriptors!!.put(name, descriptor)

        explicitlyAddedDescriptors.add(descriptor)
    }

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        addVariableOrClassDescriptor(variableDescriptor)
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        val descriptor = variableOrClassDescriptors?.get(name)
        if (descriptor is VariableDescriptor) {
            return descriptor
        }

        return workerScope.getLocalVariable(name)
    }

    override fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        if (functionGroups == null) {
            functionGroups = HashMap(1)
        }
        functionGroups!!.getOrPut(functionDescriptor.getName()) { SmartList() }.add(functionDescriptor)
        explicitlyAddedDescriptors.add(functionDescriptor)
    }

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        checkMayRead()

        val functionGroupByName = functionGroups?.get(name)
        return concatInOrder(functionGroupByName, workerScope.getFunctions(name))
    }

    override fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        addVariableOrClassDescriptor(classifierDescriptor)
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        checkMayRead()

        return variableOrClassDescriptors?.get(name) as? ClassifierDescriptor
               ?: workerScope.getClassifier(name)
    }

    override fun setImplicitReceiver(implicitReceiver: ReceiverParameterDescriptor) {
        checkMayWrite()

        if (this.implicitReceiver != null) {
            throw UnsupportedOperationException("Receiver redeclared")
        }
        this.implicitReceiver = implicitReceiver
    }

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        checkMayRead()

        if (implicitReceiverHierarchy == null) {
            implicitReceiverHierarchy = computeImplicitReceiversHierarchy()
        }
        return implicitReceiverHierarchy!!
    }

    private fun computeImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        return if (implicitReceiver != null)
            listOf(implicitReceiver!!) + super<AbstractScopeAdapter>.getImplicitReceiversHierarchy()
        else
            super<AbstractScopeAdapter>.getImplicitReceiversHierarchy()
    }

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = explicitlyAddedDescriptors

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
}
