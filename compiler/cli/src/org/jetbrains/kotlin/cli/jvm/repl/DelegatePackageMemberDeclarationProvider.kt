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

package org.jetbrains.kotlin.cli.jvm.repl

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

open class DelegatePackageMemberDeclarationProvider(var delegate: PackageMemberDeclarationProvider) : PackageMemberDeclarationProvider {
    // Can't use Kotlin delegate feature because of inability to change delegate object in runtime (KT-5870)

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean) = delegate.getAllDeclaredSubPackages(nameFilter)

    override fun getPackageFiles() = delegate.getPackageFiles()

    override fun getDeclarations(kindFilter: DescriptorKindFilter,
                                 nameFilter: (Name) -> Boolean) = delegate.getDeclarations(kindFilter, nameFilter)

    override fun getFunctionDeclarations(name: Name) = delegate.getFunctionDeclarations(name)

    override fun getPropertyDeclarations(name: Name) = delegate.getPropertyDeclarations(name)

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry> =
            delegate.getDestructuringDeclarationsEntries(name)

    override fun getClassOrObjectDeclarations(name: Name) = delegate.getClassOrObjectDeclarations(name)

    override fun getTypeAliasDeclarations(name: Name) = delegate.getTypeAliasDeclarations(name)

    override fun getDeclarationNames() = delegate.getDeclarationNames()
}
