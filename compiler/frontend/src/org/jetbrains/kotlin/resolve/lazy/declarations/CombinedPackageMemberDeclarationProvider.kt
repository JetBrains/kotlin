/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.declarations

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class CombinedPackageMemberDeclarationProvider(
        val providers: Collection<PackageMemberDeclarationProvider>
) : PackageMemberDeclarationProvider {
    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean) = providers.flatMap { it.getAllDeclaredSubPackages(nameFilter) }

    override fun getPackageFiles() = providers.flatMap { it.getPackageFiles() }

    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = providers.flatMap { it.getDeclarations(kindFilter, nameFilter) }

    override fun getFunctionDeclarations(name: Name) = providers.flatMap { it.getFunctionDeclarations(name) }

    override fun getPropertyDeclarations(name: Name) = providers.flatMap { it.getPropertyDeclarations(name) }

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry> {
        return providers.flatMap { it.getDestructuringDeclarationsEntries(name) }
    }

    override fun getClassOrObjectDeclarations(name: Name) = providers.flatMap { it.getClassOrObjectDeclarations(name) }

    override fun getTypeAliasDeclarations(name: Name) = providers.flatMap { it.getTypeAliasDeclarations(name) }

    override fun getDeclarationNames(): Set<Name> = providers.flatMapTo(HashSet()) { it.getDeclarationNames() }
}
