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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportsFactory
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SubpackagesImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.sure

open class FileScopeProviderImpl(
        private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
        private val storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val bindingTrace: BindingTrace,
        private val ktImportsFactory: KtImportsFactory
) : FileScopeProvider {

    private val defaultImports by storageManager.createLazyValue {
        ktImportsFactory.createImportDirectives(moduleDescriptor.defaultImports)
    }

    private class FileData(val scope: LexicalScope, val importResolver: ImportResolver)

    private val cache = storageManager.createMemoizedFunction { file: KtFile -> createScopeChainAndImportResolver(file) }

    override fun getFileResolutionScope(file: KtFile) = cache(file).scope

    override fun getImportResolver(file: KtFile) = cache(file).importResolver

    private fun createScopeChainAndImportResolver(file: KtFile): FileData {
        val debugName = "LazyFileScope for file " + file.name
        val tempTrace = TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve")

        val imports = file.importDirectives

        val aliasImportNames = imports.mapNotNull { if (it.aliasName != null) it.importedFqName else null }

        val packageView = moduleDescriptor.getPackage(file.packageFqName)
        val packageFragment = topLevelDescriptorProvider.getPackageFragment(file.packageFqName)
                .sure { "Could not find fragment ${file.packageFqName} for file ${file.name}" }

        fun createImportResolver(indexedImports: IndexedImports, trace: BindingTrace)
                = LazyImportResolver(storageManager, qualifiedExpressionResolver, moduleDescriptor, indexedImports, aliasImportNames, trace, packageFragment)

        val explicitImportResolver = createImportResolver(ExplicitImportsIndexed(imports), bindingTrace)
        val allUnderImportResolver = createImportResolver(AllUnderImportsIndexed(imports), bindingTrace)

        val defaultImportsFiltered = if (aliasImportNames.isEmpty()) { // optimization
            defaultImports
        }
        else {
            defaultImports.filter { it.isAllUnder || it.importedFqName !in aliasImportNames }
        }
        val defaultExplicitImportResolver = createImportResolver(ExplicitImportsIndexed(defaultImportsFiltered), tempTrace)
        val defaultAllUnderImportResolver = createImportResolver(AllUnderImportsIndexed(defaultImportsFiltered), tempTrace)

        var scope: ImportingScope

        scope = LazyImportScope(null, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "Default all under imports in $debugName (invisible classes only)")

        scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "All under imports in $debugName (invisible classes only)")

        scope = LazyImportScope(scope, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "Default all under imports in $debugName (visible classes)")

        scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "All under imports in $debugName (visible classes)")

        scope = LazyImportScope(scope, defaultExplicitImportResolver, LazyImportScope.FilteringKind.ALL,
                "Default explicit imports in $debugName")

        scope = SubpackagesImportingScope(scope, moduleDescriptor, FqName.ROOT)

        scope = packageView.memberScope.memberScopeAsImportingScope(scope) //TODO: problems with visibility too

        scope = LazyImportScope(scope, explicitImportResolver, LazyImportScope.FilteringKind.ALL, "Explicit imports in $debugName")

        val lexicalScope = LexicalScope.empty(scope, packageFragment)

        bindingTrace.recordScope(lexicalScope, file)

        val importResolver = object : ImportResolver {
            override fun forceResolveAllImports() {
                explicitImportResolver.forceResolveAllImports()
                allUnderImportResolver.forceResolveAllImports()
            }

            override fun forceResolveImport(importDirective: KtImportDirective) {
                if (importDirective.isAllUnder) {
                    allUnderImportResolver.forceResolveImport(importDirective)
                }
                else {
                    explicitImportResolver.forceResolveImport(importDirective)
                }
            }
        }

        return FileData(lexicalScope, importResolver)
    }
}
