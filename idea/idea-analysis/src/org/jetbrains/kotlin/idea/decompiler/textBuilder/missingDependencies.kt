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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.error.MissingDependencyErrorClass
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.ErrorUtils.createErrorType

private class PackageFragmentWithMissingDependencies(override val fqName: FqName, moduleDescriptor: ModuleDescriptor) :
        PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
    override fun getMemberScope(): JetScope {
        return ScopeWithMissingDependencies(fqName, this)
    }
}

private class ScopeWithMissingDependencies(val fqName: FqName, val containing: DeclarationDescriptor) : JetScopeImpl() {
    override fun getContainingDeclaration(): DeclarationDescriptor {
        return containing
    }

    override fun printScopeStructure(p: Printer) {
        p.println("Special scope for decompiler, containing class with any name")
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        return MissingDependencyErrorClassDescriptor(getContainingDeclaration(), fqName.child(name))
    }
}

private class PackageFragmentProviderForMissingDependencies(val moduleDescriptor: ModuleDescriptor) : PackageFragmentProvider {
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return listOf(PackageFragmentWithMissingDependencies(fqName, moduleDescriptor))
    }
    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        throw UnsupportedOperationException("This method is not supposed to be called.")
    }
}

private class MissingDependencyErrorClassDescriptor(
        containing: DeclarationDescriptor,
        override val fullFqName: FqName
) : MissingDependencyErrorClass, ClassDescriptorImpl(containing, fullFqName.shortName(), Modality.OPEN, listOf(), SourceElement.NO_SOURCE) {

    private val scope = ScopeWithMissingDependencies(fullFqName, this)

    ;{
        val emptyConstructor = ConstructorDescriptorImpl.create(this, Annotations.EMPTY, true, SourceElement.NO_SOURCE)
        emptyConstructor.initialize(listOf(), listOf(), Visibilities.INTERNAL)
        emptyConstructor.setReturnType(createErrorType("<ERROR RETURN TYPE>"))
        initialize(JetScope.Empty, setOf(emptyConstructor), emptyConstructor)
    }

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun getScopeForMemberLookup() = scope

    override fun getMemberScope(typeArguments: List<TypeProjection?>) = scope
}
