/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.scopes

import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.kotlin.utils.Printer

// Reads from:
// 1. Worker (a.k.a outer)
// 2. Imports

// Writes to: writable worker
public class WriteThroughScope(outerScope: JetScope, private val writableWorker: WritableScope, redeclarationHandler: RedeclarationHandler, debugName: String)
  : WritableScopeWithImports(outerScope, redeclarationHandler, debugName) {

    private var _allDescriptors: MutableCollection<DeclarationDescriptor>? = null

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> {
        checkMayRead()

        return writableWorker.getDeclarationsByLabel(labelName)
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        return writableWorker.getContainingDeclaration()
    }

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        checkMayRead()

        val result = Sets.newLinkedHashSet<FunctionDescriptor>()
        result.addAll(workerScope.getFunctions(name))
        result.addAll(super.getFunctions(name)) // Imports
        return result
    }

    override fun getProperties(name: Name): Set<VariableDescriptor> {
        checkMayRead()

        val properties = Sets.newLinkedHashSet<VariableDescriptor>()
        properties.addAll(workerScope.getProperties(name))
        properties.addAll(super.getProperties(name)) //imports
        return properties
    }

    override fun getLocalVariable(name: Name): VariableDescriptor? {
        checkMayRead()

        val variable = workerScope.getLocalVariable(name)
        if (variable != null) return variable

        return super.getLocalVariable(name) // Imports
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        checkMayRead()

        val aPackage = workerScope.getPackage(name)
        if (aPackage != null) return aPackage

        return super.getPackage(name) // Imports
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        checkMayRead()

        val classifier = workerScope.getClassifier(name)
        if (classifier != null) return classifier

        return super.getClassifier(name) // Imports
    }

    override fun addLabeledDeclaration(descriptor: DeclarationDescriptor) {
        checkMayWrite()

        writableWorker.addLabeledDeclaration(descriptor)
    }

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        checkMayWrite()

        writableWorker.addVariableDescriptor(variableDescriptor)
    }

    override fun addPropertyDescriptor(propertyDescriptor: VariableDescriptor) {
        checkMayWrite()

        writableWorker.addPropertyDescriptor(propertyDescriptor)
    }

    override fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        writableWorker.addFunctionDescriptor(functionDescriptor)
    }

    override fun addTypeParameterDescriptor(typeParameterDescriptor: TypeParameterDescriptor) {
        checkMayWrite()

        writableWorker.addTypeParameterDescriptor(typeParameterDescriptor)
    }

    override fun addClassifierDescriptor(classDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        writableWorker.addClassifierDescriptor(classDescriptor)
    }

    override fun addClassifierAlias(name: Name, classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()

        writableWorker.addClassifierAlias(name, classifierDescriptor)
    }

    override fun addPackageAlias(name: Name, packageView: PackageViewDescriptor) {
        checkMayWrite()

        writableWorker.addPackageAlias(name, packageView)
    }

    override fun addVariableAlias(name: Name, variableDescriptor: VariableDescriptor) {
        checkMayWrite()

        writableWorker.addVariableAlias(name, variableDescriptor)
    }

    override fun addFunctionAlias(name: Name, functionDescriptor: FunctionDescriptor) {
        checkMayWrite()

        writableWorker.addFunctionAlias(name, functionDescriptor)
    }

    override fun getDeclaredDescriptorsAccessibleBySimpleName(): Multimap<Name, DeclarationDescriptor> {
        return writableWorker.getDeclaredDescriptorsAccessibleBySimpleName()
    }

    override fun importScope(imported: JetScope) {
        checkMayWrite()

        super.importScope(imported)
    }

    override fun setImplicitReceiver(implicitReceiver: ReceiverParameterDescriptor) {
        checkMayWrite()

        writableWorker.setImplicitReceiver(implicitReceiver)
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        checkMayRead()

        if (_allDescriptors == null) {
            _allDescriptors = Lists.newArrayList<DeclarationDescriptor>()
            _allDescriptors!!.addAll(workerScope.getDescriptors())

            for (imported in getImports()) {
                _allDescriptors!!.addAll(imported.getDescriptors())
            }
        }
        return _allDescriptors!!
    }

    override fun printAdditionalScopeStructure(p: Printer) {
        p.print("writableWorker = ")
        writableWorker.printScopeStructure(p.withholdIndentOnce())
    }
}
