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

package org.jetbrains.jet.plugin.stubindex.resolve

import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.jet.lang.resolve.name.Name

public class CombinedPackageMemberDeclarationProvider(val providers: Collection<PackageMemberDeclarationProvider>) : PackageMemberDeclarationProvider {
    override fun getAllDeclaredSubPackages() = providers.flatMap { it.getAllDeclaredSubPackages() }

    override fun getPackageFiles() = providers.flatMap { it.getPackageFiles() }

    override fun getAllDeclarations() = providers.flatMap { it.getAllDeclarations() }

    override fun getFunctionDeclarations(name: Name) = providers.flatMap { it.getFunctionDeclarations(name) }

    override fun getPropertyDeclarations(name: Name) = providers.flatMap { it.getPropertyDeclarations(name) }

    override fun getClassOrObjectDeclarations(name: Name) = providers.flatMap { it.getClassOrObjectDeclarations(name) }
}
