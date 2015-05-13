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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.utils.sure
import java.util.ArrayList

class LazyFileScope private constructor(
        scopeChain: List<JetScope>,
        private val aliasImportResolver: LazyImportResolver,
        private val allUnderImportResolver: LazyImportResolver,
        containingDeclaration: PackageFragmentDescriptor,
        debugName: String
) : ChainedScope(containingDeclaration, debugName, *scopeChain.toTypedArray()) {

    public fun forceResolveAllImports() {
        aliasImportResolver.forceResolveAllContents()
        allUnderImportResolver.forceResolveAllContents()
    }

    public fun forceResolveImport(importDirective: JetImportDirective) {
        if (importDirective.isAllUnder()) {
            allUnderImportResolver.forceResolveImportDirective(importDirective)
        }
        else {
            aliasImportResolver.forceResolveImportDirective(importDirective)
        }
    }

    companion object Factory {
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
            val moduleDescriptor = resolveSession.getModuleDescriptor()
            val packageFragment = resolveSession.getPackageFragment(file.getPackageFqName())
                    .sure { "Could not find fragment ${file.getPackageFqName()} for file ${file.getName()}" }

            val aliasImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AliasImportsIndexed(imports), traceForImportResolve, true)
            val allUnderImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AllUnderImportsIndexed(imports), traceForImportResolve, true)
            val defaultAliasImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AliasImportsIndexed(defaultImports), traceForDefaultImportResolve, false)
            val defaultAllUnderImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AllUnderImportsIndexed(defaultImports), traceForDefaultImportResolve, false)

            val scopeChain = ArrayList<JetScope>()

            scopeChain.add(LazyImportScope(packageFragment, aliasImportResolver, LazyImportScope.FilteringKind.ALL, "Alias imports in $debugName"))

            scopeChain.add(NoSubpackagesInPackageScope(packageView)) //TODO: problems with visibility too
            scopeChain.add(JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor))

            scopeChain.add(LazyImportScope(packageFragment, defaultAliasImportResolver, LazyImportScope.FilteringKind.ALL, "Default alias imports in $debugName"))

            scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "Default all under imports in $debugName (visible classes)"))
            scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "All under imports in $debugName (visible classes)"))

            scopeChain.addAll(additionalScopes)

            scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "Default all under imports in $debugName (invisible classes only)"))
            scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "All under imports in $debugName (invisible classes only)"))

            return LazyFileScope(scopeChain, aliasImportResolver, allUnderImportResolver, packageFragment, debugName)
        }

        private fun getPackageViewDescriptor(file: JetFile, resolveSession: ResolveSession): PackageViewDescriptor {
            val fqName = file.getPackageFqName()
            return resolveSession.getModuleDescriptor().getPackage(fqName)
                ?: throw IllegalStateException("Package not found: $fqName maybe the file is not in scope of this resolve session: ${file.getName()}")
        }
    }
}
