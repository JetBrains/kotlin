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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class LazyPackageMemberScope(
        private val resolveSession: ResolveSession,
        declarationProvider: PackageMemberDeclarationProvider,
        thisPackage: PackageFragmentDescriptor)
: AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider>(resolveSession, declarationProvider, thisPackage, resolveSession.trace) {

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
    }

    override fun getScopeForMemberDeclarationResolution(declaration: KtDeclaration) =
            resolveSession.fileScopeProvider.getFileResolutionScope(declaration.containingKtFile)

    override fun getScopeForInitializerResolution(declaration: KtDeclaration) =
            getScopeForMemberDeclarationResolution(declaration)

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>) {
        // No extra classes
    }

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        // No extra functions
    }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        // No extra properties
    }

    override fun recordLookup(name: Name, from: LookupLocation) {
        c.lookupTracker.record(from, thisDescriptor, name)
    }

    override fun getClassifierNames(): Set<Name>? = declarationProvider.getDeclarationNames()
    override fun getFunctionNames() = declarationProvider.getDeclarationNames()
    override fun getVariableNames() = declarationProvider.getDeclarationNames()

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for package " + thisDescriptor.name
}
