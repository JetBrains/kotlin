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
    private val declaredDescriptorsAccessibleBySimpleName = HashMultimap.create<Name, DeclarationDescriptor>()

    private var functionGroups: SetMultimap<Name, FunctionDescriptor>? = null

    private var variableOrClassDescriptors: MutableMap<Name, DeclarationDescriptor>? = null

    private var propertyGroups: SetMultimap<Name, VariableDescriptor>? = null

    private var packageAliases: MutableMap<Name, PackageViewDescriptor>? = null

    private var labelsToDescriptors: MutableMap<Name, MutableList<DeclarationDescriptor>>? = null

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

    private fun getLabelsToDescriptors(): MutableMap<Name, MutableList<DeclarationDescriptor>> {
        if (labelsToDescriptors == null) {
            labelsToDescriptors = HashMap()
        }
        return labelsToDescriptors!!
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

        val labelsToDescriptors = getLabelsToDescriptors()
        val name = descriptor.getName()
        var declarationDescriptors = labelsToDescriptors.getOrPut(name) { ArrayList() }
        declarationDescriptors.add(descriptor)
    }

    private fun getVariableOrClassDescriptors(): MutableMap<Name, DeclarationDescriptor> {
        if (variableOrClassDescriptors == null) {
            variableOrClassDescriptors = HashMap()
        }
        return variableOrClassDescriptors!!
    }

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        addVariableDescriptor(variableDescriptor, false)
    }

    private fun addVariableDescriptor(variableDescriptor: VariableDescriptor, isProperty: Boolean) {
        checkMayWrite()

        val name = variableDescriptor.getName()
        if (isProperty) {
            checkForPropertyRedeclaration(name, variableDescriptor)
            getPropertyGroups().put(name, variableDescriptor)
        }
        if (variableDescriptor.getExtensionReceiverParameter() == null) {
            checkForRedeclaration(name, variableDescriptor)
            // TODO : Should this always happen?
            getVariableOrClassDescriptors().put(name, variableDescriptor)
        }
        explicitlyAddedDescriptors.add(variableDescriptor)
        addToDeclared(variableDescriptor)
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> {
        checkMayRead()

        val propertyGroupsByName = propertyGroups?.get(name) ?: return workerScope.getProperties(name)
        return concatInOrder(propertyGroupsByName, workerScope.getProperties(name))
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        val descriptor = variableOrClassDescriptors?.get(name)
        if (descriptor is VariableDescriptor && propertyGroups?.get(name)?.contains(descriptor) != true) {
            return descriptor
        }

        return workerScope.getLocalVariable(name)
    }

    private fun getPropertyGroups(): SetMultimap<Name, VariableDescriptor> {
        if (propertyGroups == null) {
            propertyGroups = LinkedHashMultimap.create()
        }
        return propertyGroups!!
    }

    private fun getFunctionGroups(): SetMultimap<Name, FunctionDescriptor> {
        if (functionGroups == null) {
            functionGroups = LinkedHashMultimap.create()
        }
        return functionGroups!!
    }

    override fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        getFunctionGroups().put(functionDescriptor.getName(), functionDescriptor)
        explicitlyAddedDescriptors.add(functionDescriptor)
    }

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        checkMayRead()

        val functionGroupByName = functionGroups?.get(name)
        return concatInOrder(functionGroupByName, workerScope.getFunctions(name))
    }

    override fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        val name = classifierDescriptor.getName()
        checkForRedeclaration(name, classifierDescriptor)
        getVariableOrClassDescriptors().put(name, classifierDescriptor)
        explicitlyAddedDescriptors.add(classifierDescriptor)
        addToDeclared(classifierDescriptor)
    }

    private fun checkForPropertyRedeclaration(name: Name, variableDescriptor: VariableDescriptor) {
        val properties = getPropertyGroups()[name]
        val receiverParameter = variableDescriptor.getExtensionReceiverParameter()
        for (oldProperty in properties) {
            val receiverParameterForOldVariable = oldProperty.getExtensionReceiverParameter()
            if (receiverParameter != null
                    && receiverParameterForOldVariable != null
                    && JetTypeChecker.DEFAULT.equalTypes(receiverParameter.getType(), receiverParameterForOldVariable.getType())) {
                redeclarationHandler.handleRedeclaration(oldProperty, variableDescriptor)
            }
        }
    }

    private fun checkForRedeclaration(name: Name, classifierDescriptor: DeclarationDescriptor) {
        val originalDescriptor = getVariableOrClassDescriptors()[name]
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, classifierDescriptor)
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        checkMayRead()

        return variableOrClassDescriptors?.get(name) as? ClassifierDescriptor
               ?: workerScope.getClassifier(name)
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        checkMayRead()

        return packageAliases?.get(name)
               ?: workerScope.getPackage(name)
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

    private fun addToDeclared(descriptor: DeclarationDescriptor) {
        declaredDescriptorsAccessibleBySimpleName.put(descriptor.getName(), descriptor)
    }

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor>
            = declaredDescriptorsAccessibleBySimpleName.values()

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
