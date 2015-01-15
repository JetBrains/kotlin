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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.psi.JetCodeFragment

class LazyFileScope private(
        private val aliasImportsScope: LazyImportScope,
        private val allUnderImportsScope: LazyImportScope,
        private val defaultAliasImportsScope: LazyImportScope,
        private val defaultAllUnderImportsScope: LazyImportScope,
        containingDeclaration: PackageViewDescriptor,
        debugName: String
) : ChainedScope(containingDeclaration, debugName, aliasImportsScope, allUnderImportsScope, defaultAliasImportsScope, defaultAllUnderImportsScope) {

    public fun forceResolveAllContents() {
        aliasImportsScope.forceResolveAllContents()
        allUnderImportsScope.forceResolveAllContents()
    }

    public fun forceResolveImportDirective(importDirective: JetImportDirective) {
        if (importDirective.isAllUnder()) {
            allUnderImportsScope.forceResolveImportDirective(importDirective)
        }
        else {
            aliasImportsScope.forceResolveImportDirective(importDirective)
        }
    }

    class object {
        public fun create(
                resolveSession: ResolveSession,
                packageDescriptor: PackageViewDescriptor,
                jetFile: JetFile,
                defaultImports: Collection<JetImportDirective>,
                traceForImportResolve: BindingTrace,
                traceForDefaultImportResolve: BindingTrace,
                debugName: String
        ): LazyFileScope {
            val imports = if (jetFile is JetCodeFragment)
                jetFile.importsAsImportList()?.getImports() ?: listOf()
            else
                jetFile.getImportDirectives()

            val aliasImportsScope = LazyImportScope(resolveSession, packageDescriptor, AliasImportsIndexed(imports), traceForImportResolve, "Alias imports in $debugName")
            val allUnderImportsScope = LazyImportScope(resolveSession, packageDescriptor, AllUnderImportsIndexed(imports), traceForImportResolve, "All under imports in $debugName")
            val defaultAliasImportsScope = LazyImportScope(resolveSession, packageDescriptor, AliasImportsIndexed(defaultImports), traceForDefaultImportResolve, "Default alias imports in $debugName")
            val defaultAllUnderImportsScope = LazyImportScope(resolveSession, packageDescriptor, AllUnderImportsIndexed(defaultImports), traceForDefaultImportResolve, "Default all under imports in $debugName")
            return LazyFileScope(aliasImportsScope, allUnderImportsScope, defaultAliasImportsScope, defaultAllUnderImportsScope, packageDescriptor, debugName)
        }
    }
}
