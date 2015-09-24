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
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportsFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.get
import org.jetbrains.kotlin.utils.sure
import java.util.*

public class FileScopeProviderImpl(
        private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
        private val storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val bindingTrace: BindingTrace,
        private val jetImportsFactory: JetImportsFactory,
        private val additionalScopes: Iterable<FileScopeProvider.AdditionalScopes>
) : FileScopeProvider {

    private val defaultImports by storageManager.createLazyValue {
        jetImportsFactory.createImportDirectives(moduleDescriptor.defaultImports)
    }

    private val fileScopes = storageManager.createMemoizedFunction { file: JetFile -> createFileScope(file) }

    override fun getFileScope(file: JetFile) = fileScopes(file)

    private fun createFileScope(file: JetFile): LazyFileScope {
        val debugName = "LazyFileScope for file " + file.getName()
        val tempTrace = TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve")

        val imports = if (file is JetCodeFragment)
            file.importsAsImportList()?.getImports() ?: listOf()
        else
            file.getImportDirectives()

        val packageView = moduleDescriptor.getPackage(file.getPackageFqName())
        val packageFragment = topLevelDescriptorProvider.getPackageFragment(file.getPackageFqName())
                .sure { "Could not find fragment ${file.getPackageFqName()} for file ${file.getName()}" }

        fun createImportResolver(indexedImports: IndexedImports, trace: BindingTrace)
                = LazyImportResolver(storageManager, qualifiedExpressionResolver, moduleDescriptor, indexedImports, trace, packageFragment)

        val aliasImportResolver = createImportResolver(AliasImportsIndexed(imports), bindingTrace)
        val allUnderImportResolver = createImportResolver(AllUnderImportsIndexed(imports), bindingTrace)
        val defaultAliasImportResolver = createImportResolver(AliasImportsIndexed(defaultImports), tempTrace)
        val defaultAllUnderImportResolver = createImportResolver(AllUnderImportsIndexed(defaultImports), tempTrace)

        val scopeChain = ArrayList<JetScope>()

        scopeChain.add(LazyImportScope(packageFragment, aliasImportResolver, LazyImportScope.FilteringKind.ALL, "Alias imports in $debugName"))

        scopeChain.add(NoSubpackagesInPackageScope(packageView)) //TODO: problems with visibility too
        scopeChain.add(SubpackagesScope(moduleDescriptor, FqName.ROOT))

        scopeChain.add(LazyImportScope(packageFragment, defaultAliasImportResolver, LazyImportScope.FilteringKind.ALL, "Default alias imports in $debugName"))

        scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "All under imports in $debugName (visible classes)"))
        scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "Default all under imports in $debugName (visible classes)"))

        scopeChain.addAll(additionalScopes.flatMap { it.scopes })

        scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "All under imports in $debugName (invisible classes only)"))
        scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "Default all under imports in $debugName (invisible classes only)"))

        return LazyFileScope(scopeChain, aliasImportResolver, allUnderImportResolver, packageFragment, debugName)
    }
}
