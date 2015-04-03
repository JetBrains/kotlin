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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.Printer

class SingleImportScope(private val aliasName: Name, private val descriptors: Collection<DeclarationDescriptor>) : JetScope {
    override fun getClassifier(name: Name)
            = if (name == aliasName) descriptors.filterIsInstance<ClassifierDescriptor>().singleOrNull() else null

    override fun getPackage(name: Name)
            = if (name == aliasName) descriptors.filterIsInstance<PackageViewDescriptor>().singleOrNull() else null

    override fun getProperties(name: Name)
            = if (name == aliasName) descriptors.filterIsInstance<VariableDescriptor>() else emptyList()

    override fun getFunctions(name: Name)
            = if (name == aliasName) descriptors.filterIsInstance<FunctionDescriptor>() else emptyList()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null

    override fun getContainingDeclaration(): DeclarationDescriptor = throw UnsupportedOperationException()

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = emptyList()

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = descriptors

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = emptyList()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", aliasName)
    }
}