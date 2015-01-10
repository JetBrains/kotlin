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

package org.jetbrains.jet.lang.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindFilter

public class LazyPackageMemberScope(
        resolveSession: ResolveSession,
        declarationProvider: PackageMemberDeclarationProvider,
        thisPackage: PackageFragmentDescriptor)
: AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider>(resolveSession, declarationProvider, thisPackage, resolveSession.getTrace()) {

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter)
    }

    override fun getPackage(name: Name): PackageViewDescriptor? = null

    override fun getScopeForMemberDeclarationResolution(declaration: JetDeclaration)
            = resolveSession.getScopeProvider().getFileScope(declaration.getContainingJetFile())

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<FunctionDescriptor>) {
        // No extra functions
    }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<VariableDescriptor>) {
        // No extra properties
    }

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for package " + thisDescriptor.getName()
}
