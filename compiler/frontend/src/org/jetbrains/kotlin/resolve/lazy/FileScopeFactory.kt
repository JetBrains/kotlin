/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportsFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SubpackagesImportingScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.script.getScriptExternalDependencies
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

data class FileScopes(val lexicalScope: LexicalScope, val importingScope: ImportingScope, val importResolver: ImportResolver)

class FileScopeFactory(
        private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
        private val storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val bindingTrace: BindingTrace,
        private val ktImportsFactory: KtImportsFactory,
        private val platformToKotlinClassMap: PlatformToKotlinClassMap,
        private val defaultImportProvider: DefaultImportProvider,
        private val languageVersionSettings: LanguageVersionSettings,
        private val deprecationResolver: DeprecationResolver
) {
    /* avoid constructing psi for default imports prematurely (time consuming in some scenarios) */
    private val defaultImports by storageManager.createLazyValue {
        ktImportsFactory.createImportDirectives(defaultImportProvider.defaultImports)
    }

    fun createScopesForFile(file: KtFile, existingImports: ImportingScope? = null): FileScopes {
        val packageView = moduleDescriptor.getPackage(file.packageFqName)
        val packageFragment = topLevelDescriptorProvider.getPackageFragment(file.packageFqName)
        if (packageFragment == null) {
            // TODO J2K and change return type of diagnoseMissingPackageFragment() to Nothing
            (topLevelDescriptorProvider as? LazyClassContext)?.declarationProviderFactory?.diagnoseMissingPackageFragment(file)
            error("Could not find fragment ${file.packageFqName} for file ${file.name}")
        }

        return FilesScopesBuilder(file, existingImports, packageFragment, packageView).result
    }

    private inner class FilesScopesBuilder(
            private val file: KtFile,
            private val existingImports: ImportingScope?,
            private val packageFragment: PackageFragmentDescriptor,
            private val packageView: PackageViewDescriptor
    ) {
        val imports = file.importDirectives
        val aliasImportNames = imports.mapNotNull { if (it.aliasName != null) it.importedFqName else null }

        val explicitImportResolver = createImportResolver(ExplicitImportsIndexed(imports), bindingTrace)
        val allUnderImportResolver = createImportResolver(AllUnderImportsIndexed(imports), bindingTrace) // TODO: should we count excludedImports here also?

        val lazyImportingScope = object : ImportingScope by ImportingScope.Empty {
            // avoid constructing the scope before we query it
            override val parent: ImportingScope by storageManager.createLazyValue {
                createImportingScope()
            }
        }

        val lexicalScope = LexicalScope.Base(lazyImportingScope, topLevelDescriptorProvider.getPackageFragment(file.packageFqName)!!)

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

        val result = FileScopes(lexicalScope, lazyImportingScope, importResolver)

        fun createImportResolver(indexedImports: IndexedImports, trace: BindingTrace, excludedImports: List<FqName>? = null) =
                LazyImportResolver(
                        storageManager, qualifiedExpressionResolver, moduleDescriptor, platformToKotlinClassMap, languageVersionSettings,
                        indexedImports, aliasImportNames concat excludedImports, trace, packageFragment,
                        deprecationResolver
                )


        fun createImportingScope(): LazyImportScope {
            val tempTrace = TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve", false)

            val extraImports = file.originalFile.virtualFile?.let { vFile ->
                val scriptExternalDependencies = getScriptExternalDependencies(vFile, file.project)
                ktImportsFactory.createImportDirectives(scriptExternalDependencies?.imports?.map { ImportPath.fromString(it) }.orEmpty())
            }

            val allImplicitImports = defaultImports concat extraImports

            val defaultImportsFiltered = if (aliasImportNames.isEmpty()) { // optimization
                allImplicitImports
            }
            else {
                allImplicitImports.filter { it.isAllUnder || it.importedFqName !in aliasImportNames }
            }

            val defaultExplicitImportResolver = createImportResolver(ExplicitImportsIndexed(defaultImportsFiltered), tempTrace)
            val defaultAllUnderImportResolver = createImportResolver(AllUnderImportsIndexed(defaultImportsFiltered), tempTrace, defaultImportProvider.excludedImports)

            val dummyContainerDescriptor = DummyContainerDescriptor(file, packageFragment)

            var scope: ImportingScope

            val debugName = "LazyFileScope for file " + file.name
            scope = LazyImportScope(existingImports, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                                    "Default all under imports in $debugName (invisible classes only)")

            scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                                    "All under imports in $debugName (invisible classes only)")

            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.INVISIBLE_CLASSES, scope)

            scope = LazyImportScope(scope, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                                    "Default all under imports in $debugName (visible classes)")

            scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                                    "All under imports in $debugName (visible classes)")

            scope = LazyImportScope(scope, defaultExplicitImportResolver, LazyImportScope.FilteringKind.ALL,
                                    "Default explicit imports in $debugName")

            scope = SubpackagesImportingScope(scope, moduleDescriptor, FqName.ROOT)

            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.VISIBLE_CLASSES, scope)

            return LazyImportScope(scope, explicitImportResolver, LazyImportScope.FilteringKind.ALL, "Explicit imports in $debugName")
        }

        private infix fun <T> Collection<T>.concat(other: Collection<T>?) =
                if (other == null || other.isEmpty()) this else this + other
    }

    private enum class FilteringKind {
        VISIBLE_CLASSES, INVISIBLE_CLASSES
    }

    private fun currentPackageScope(
            packageView: PackageViewDescriptor,
            aliasImportNames: Collection<FqName>,
            fromDescriptor: DummyContainerDescriptor,
            filteringKind: FilteringKind,
            parentScope: ImportingScope
    ): ImportingScope {
        val scope = packageView.memberScope
        val packageName = packageView.fqName
        val excludedNames = aliasImportNames.mapNotNull { if (it.parent() == packageName) it.shortName() else null }

        return object : ImportingScope {
            override val parent: ImportingScope? = parentScope

            override fun getContributedPackage(name: Name) = null

            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
                if (name in excludedNames) return null
                val classifier = scope.getContributedClassifier(name, location) ?: return null
                val visible = Visibilities.isVisibleIgnoringReceiver(classifier as DeclarationDescriptorWithVisibility, fromDescriptor)
                return classifier.takeIf { filteringKind == if (visible) FilteringKind.VISIBLE_CLASSES else FilteringKind.INVISIBLE_CLASSES }
            }

            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
                if (name in excludedNames) return emptyList()
                return scope.getContributedVariables(name, location)
            }

            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
                if (name in excludedNames) return emptyList()
                return scope.getContributedFunctions(name, location)
            }

            override fun getContributedDescriptors(
                    kindFilter: DescriptorKindFilter,
                    nameFilter: (Name) -> Boolean,
                    changeNamesForAliased: Boolean
            ): Collection<DeclarationDescriptor> {
                // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
                return scope.getContributedDescriptors(
                        kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK),
                        { name -> name !in excludedNames && nameFilter(name) }
                ).filter { it !is PackageViewDescriptor } // subpackages of the current package not accessible by the short name
            }

            override fun toString() = "Scope for current package (${filteringKind.name})"

            override fun printStructure(p: Printer) {
                p.println(this.toString())
            }
        }
    }

    // we use this dummy implementation of DeclarationDescriptor to check accessibility of symbols from the current package
    private class DummyContainerDescriptor(file: KtFile, private val packageFragment: PackageFragmentDescriptor) : DeclarationDescriptorNonRoot {
        private val sourceElement = KotlinSourceElement(file)

        override fun getContainingDeclaration() = packageFragment

        override fun getSource() = sourceElement

        override fun getOriginal() = this
        override val annotations: Annotations get() = Annotations.EMPTY

        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
            throw UnsupportedOperationException()
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
            throw UnsupportedOperationException()
        }

        override fun getName(): Name {
            throw UnsupportedOperationException()
        }
    }
}
