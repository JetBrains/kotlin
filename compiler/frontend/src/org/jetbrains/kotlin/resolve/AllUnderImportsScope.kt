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
import org.jetbrains.kotlin.resolve.scopes.UsageLocation
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.Printer
import java.util.ArrayList

class AllUnderImportsScope : JetScope {
    private val scopes = ArrayList<JetScope>()

    public fun addAllUnderImport(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageViewDescriptor) {
            scopes.add(NoSubpackagesInPackageScope(descriptor))
        }
        else if (descriptor is ClassDescriptor && ImportDirectiveProcessor.canAllUnderImportFromClass(descriptor)) {
            scopes.add(descriptor.getStaticScope())
            scopes.add(descriptor.getUnsubstitutedInnerClassesScope())
        }
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return scopes.flatMap { it.getDescriptors(kindFilter, nameFilter) }
    }

    override fun getClassifier(name: Name, location: UsageLocation): ClassifierDescriptor? {
        return scopes.asSequence().map { it.getClassifier(name, location) }.filterNotNull().singleOrNull()
    }

    override fun getProperties(name: Name, location: UsageLocation): Collection<VariableDescriptor> {
        return scopes.flatMap { it.getProperties(name, location) }
    }

    override fun getFunctions(name: Name, location: UsageLocation): Collection<FunctionDescriptor> {
        return scopes.flatMap { it.getFunctions(name, location) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name): Collection<PropertyDescriptor> {
        return scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes, name) }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name): Collection<FunctionDescriptor> {
        return scopes.flatMap { it.getSyntheticExtensionFunctions(receiverTypes, name) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor> {
        return scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes) }
    }

    override fun getPackage(name: Name): PackageViewDescriptor? = null // packages are not imported by all under imports

    override fun getLocalVariable(name: Name): VariableDescriptor? = null

    override fun getContainingDeclaration(): DeclarationDescriptor = throw UnsupportedOperationException()

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = listOf()

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = listOf()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName())
    }
}

