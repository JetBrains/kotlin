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

// Reads from:
// 1. Maps
// 2. Worker
// 3. Imports

// Writes to: maps

public class WritableScopeImpl(scope: JetScope,
                               private val ownerDeclarationDescriptor: DeclarationDescriptor,
                               redeclarationHandler: RedeclarationHandler,
                               debugName: String)
: WritableScopeWithImports(scope, redeclarationHandler, debugName) {

    private val explicitlyAddedDescriptors = SmartList<DeclarationDescriptor>()
    private val declaredDescriptorsAccessibleBySimpleName = HashMultimap.create<Name, DeclarationDescriptor>()

    private var functionGroups: SetMultimap<Name, FunctionDescriptor>? = null

    private var variableOrClassDescriptors: MutableMap<Name, DeclarationDescriptor>? = null

    private var propertyGroups: SetMultimap<Name, VariableDescriptor>? = null

    private var packageAliases: MutableMap<Name, PackageViewDescriptor>? = null

    private var labelsToDescriptors: MutableMap<Name, MutableList<DeclarationDescriptor>>? = null

    private var implicitReceiver: ReceiverParameterDescriptor? = null

    override fun getContainingDeclaration(): DeclarationDescriptor = ownerDeclarationDescriptor

    override fun importScope(imported: JetScope) {
        checkMayWrite()
        super.importScope(imported)
    }

    override fun importClassifierAlias(importedClassifierName: Name, classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        explicitlyAddedDescriptors.add(classifierDescriptor)
        super.importClassifierAlias(importedClassifierName, classifierDescriptor)
    }

    override fun importPackageAlias(aliasName: Name, packageView: PackageViewDescriptor) {
        checkMayWrite()

        explicitlyAddedDescriptors.add(packageView)
        super.importPackageAlias(aliasName, packageView)
    }

    override fun importFunctionAlias(aliasName: Name, functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        addFunctionDescriptor(functionDescriptor)
        super.importFunctionAlias(aliasName, functionDescriptor)

    }

    override fun importVariableAlias(aliasName: Name, variableDescriptor: VariableDescriptor) {
        checkMayWrite()

        addPropertyDescriptor(variableDescriptor)
        super.importVariableAlias(aliasName, variableDescriptor)
    }

    override fun clearImports() {
        checkMayWrite()

        super.clearImports()
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        checkMayRead()
        changeLockLevel(WritableScope.LockLevel.READING)

        val result = ArrayList<DeclarationDescriptor>()
        result.addAll(explicitlyAddedDescriptors)
        result.addAll(workerScope.getDescriptors(kindFilter, nameFilter))
        getImports().flatMapTo(result) { it.getDescriptors(kindFilter, nameFilter) }
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

        val superResult = super.getDeclarationsByLabel(labelName)
        val declarationDescriptors = getLabelsToDescriptors()[labelName]
        if (declarationDescriptors == null) return superResult
        if (superResult.isEmpty()) return declarationDescriptors
        return declarationDescriptors + superResult
    }

    override fun addLabeledDeclaration(descriptor: DeclarationDescriptor) {
        checkMayWrite()

        val labelsToDescriptors = getLabelsToDescriptors()
        val name = descriptor.getName()
        var declarationDescriptors = labelsToDescriptors[name]
        if (declarationDescriptors == null) {
            declarationDescriptors = ArrayList()
            labelsToDescriptors.put(name, declarationDescriptors!!)
        }
        declarationDescriptors!!.add(descriptor)
    }

    private fun getVariableOrClassDescriptors(): MutableMap<Name, DeclarationDescriptor> {
        if (variableOrClassDescriptors == null) {
            variableOrClassDescriptors = HashMap()
        }
        return variableOrClassDescriptors!!
    }

    private fun getPackageAliases(): MutableMap<Name, PackageViewDescriptor> {
        if (packageAliases == null) {
            packageAliases = HashMap()
        }
        return packageAliases!!
    }

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        addVariableDescriptor(variableDescriptor, false)
    }

    override fun addPropertyDescriptor(propertyDescriptor: VariableDescriptor) {
        addVariableDescriptor(propertyDescriptor, true)
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

    override fun getProperties(name: Name): Set<VariableDescriptor> {
        checkMayRead()

        val result = Sets.newLinkedHashSet(getPropertyGroups().get(name))

        result.addAll(workerScope.getProperties(name))

        result.addAll(super.getProperties(name))

        return result
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        val descriptor = getVariableOrClassDescriptors()[name]
        if (descriptor is VariableDescriptor && !getPropertyGroups()[name].contains(descriptor)) {
            return descriptor
        }

        return workerScope.getLocalVariable(name) ?: super.getLocalVariable(name)
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

        val result = Sets.newLinkedHashSet(getFunctionGroups().get(name))

        result.addAll(workerScope.getFunctions(name))

        result.addAll(super.getFunctions(name))

        return result
    }

    override fun addTypeParameterDescriptor(typeParameterDescriptor: TypeParameterDescriptor) {
        checkMayWrite()

        addClassifierAlias(typeParameterDescriptor.getName(), typeParameterDescriptor)
    }

    override fun addClassifierDescriptor(classDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        addClassifierAlias(classDescriptor.getName(), classDescriptor)
    }

    override fun addClassifierAlias(name: Name, classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        checkForRedeclaration(name, classifierDescriptor)
        getVariableOrClassDescriptors().put(name, classifierDescriptor)
        explicitlyAddedDescriptors.add(classifierDescriptor)
        addToDeclared(classifierDescriptor)
    }

    override fun addPackageAlias(name: Name, packageView: PackageViewDescriptor) {
        checkMayWrite()

        checkForRedeclaration(name, packageView)
        getPackageAliases().put(name, packageView)
        explicitlyAddedDescriptors.add(packageView)
        addToDeclared(packageView)
    }

    override fun addFunctionAlias(name: Name, functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        checkForRedeclaration(name, functionDescriptor)
        getFunctionGroups().put(name, functionDescriptor)
        explicitlyAddedDescriptors.add(functionDescriptor)
    }

    override fun addVariableAlias(name: Name, variableDescriptor: VariableDescriptor) {
        checkMayWrite()

        checkForRedeclaration(name, variableDescriptor)

        getVariableOrClassDescriptors().put(name, variableDescriptor)
        getPropertyGroups().put(name, variableDescriptor)

        explicitlyAddedDescriptors.add(variableDescriptor)
        addToDeclared(variableDescriptor)
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

        return getVariableOrClassDescriptors()[name] as? ClassifierDescriptor
               ?: workerScope.getClassifier(name)
               ?: super.getClassifier(name)
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        checkMayRead()

        return getPackageAliases().get(name)
               ?: workerScope.getPackage(name)
               ?: super.getPackage(name)
    }

    override fun setImplicitReceiver(implicitReceiver: ReceiverParameterDescriptor) {
        checkMayWrite()

        if (this.implicitReceiver != null) {
            throw UnsupportedOperationException("Receiver redeclared")
        }
        this.implicitReceiver = implicitReceiver
    }

    override fun computeImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        return if (implicitReceiver != null)
            listOf(implicitReceiver!!) + super.computeImplicitReceiversHierarchy()
        else
            super.computeImplicitReceiversHierarchy()
    }

    private fun addToDeclared(descriptor: DeclarationDescriptor) {
        declaredDescriptorsAccessibleBySimpleName.put(descriptor.getName(), descriptor)
    }

    override fun getDeclaredDescriptorsAccessibleBySimpleName(): Multimap<Name, DeclarationDescriptor>
            = declaredDescriptorsAccessibleBySimpleName

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor>
            = declaredDescriptorsAccessibleBySimpleName.values()

    override fun printAdditionalScopeStructure(p: Printer) {
    }
}
