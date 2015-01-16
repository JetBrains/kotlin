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
import org.jetbrains.kotlin.resolve.JetModuleUtil
import org.jetbrains.kotlin.resolve.NoSubpackagesInPackageScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName

class LazyFileScope private(
        private val aliasImportsScope: LazyImportScope,
        private val allUnderImportsScope: LazyImportScope,
        private val defaultAliasImportsScope: LazyImportScope,
        private val defaultAllUnderImportsScope: LazyImportScope,
        currentPackageMembersScope: JetScope,
        rootPackagesScope: JetScope,
        additionalScopes: List<JetScope>,
        containingDeclaration: PackageFragmentDescriptor,
        debugName: String
) : ChainedScope(containingDeclaration,
                 debugName,
                 *(listOf(aliasImportsScope, defaultAliasImportsScope, currentPackageMembersScope, rootPackagesScope, allUnderImportsScope, defaultAllUnderImportsScope) + additionalScopes).copyToArray()) {

    public fun forceResolveAllImports() {
        aliasImportsScope.forceResolveAllContents()
        allUnderImportsScope.forceResolveAllContents()
    }

    public fun forceResolveImport(importDirective: JetImportDirective) {
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
                file: JetFile,
                defaultImports: Collection<JetImportDirective>,
                additionalScopes: List<JetScope>,
                traceForImportResolve: BindingTrace,
                traceForDefaultImportResolve: BindingTrace,
                debugName: String
        ): LazyFileScope {
            val imports = if (file is JetCodeFragment)
                file.importsAsImportList()?.getImports() ?: listOf()
            else
                file.getImportDirectives()

            val packageView = getPackageViewDescriptor(file, resolveSession)
            val inRootPackage = packageView.getFqName().isRoot()
            val rootPackageView = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT)
                                  ?: throw IllegalStateException("Root package not found")

            val currentPackageMembersScope = NoSubpackagesInPackageScope(packageView)
            val rootPackagesScope = JetModuleUtil.getSubpackagesOfRootScope(resolveSession.getModuleDescriptor())
            val aliasImportsScope = LazyImportScope(resolveSession, packageView, AliasImportsIndexed(imports), traceForImportResolve, "Alias imports in $debugName", inRootPackage)
            val allUnderImportsScope = LazyImportScope(resolveSession, packageView, AllUnderImportsIndexed(imports), traceForImportResolve, "All under imports in $debugName", inRootPackage)
            val defaultAliasImportsScope = LazyImportScope(resolveSession, rootPackageView, AliasImportsIndexed(defaultImports), traceForDefaultImportResolve, "Default alias imports in $debugName", false)
            val defaultAllUnderImportsScope = LazyImportScope(resolveSession, rootPackageView, AllUnderImportsIndexed(defaultImports), traceForDefaultImportResolve, "Default all under imports in $debugName", false)

            return LazyFileScope(aliasImportsScope, allUnderImportsScope, defaultAliasImportsScope, defaultAllUnderImportsScope,
                                 currentPackageMembersScope, rootPackagesScope, additionalScopes,
                                 resolveSession.getPackageFragment(file.getPackageFqName()), debugName)
        }

        private fun getPackageViewDescriptor(file: JetFile, resolveSession: ResolveSession): PackageViewDescriptor {
            val fqName = file.getPackageFqName()
            return resolveSession.getModuleDescriptor().getPackage(fqName)
                ?: throw IllegalStateException("Package not found: $fqName maybe the file is not in scope of this resolve session: ${file.getName()}")
        }
    }
}
