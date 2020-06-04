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

import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

data class FileScopes(val lexicalScope: LexicalScope, val importingScope: ImportingScope, val importForceResolver: ImportForceResolver)

class FileScopeFactory(
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val bindingTrace: BindingTrace,
    private val analyzerServices: PlatformDependentAnalyzerServices,
    private val components: ImportResolutionComponents
) {
    private val defaultImports =
        analyzerServices.getDefaultImports(components.languageVersionSettings, includeLowPriorityImports = false).map(::DefaultImportImpl)

    private val defaultLowPriorityImports = analyzerServices.defaultLowPriorityImports.map(::DefaultImportImpl)

    private class DefaultImportImpl(private val importPath: ImportPath) : KtImportInfo {
        override val isAllUnder: Boolean get() = importPath.isAllUnder

        override val importContent = KtImportInfo.ImportContent.FqNameBased(importPath.fqName)

        override val aliasName: String? get() = importPath.alias?.asString()

        override val importedFqName: FqName? get() = importPath.fqName
    }

    fun createScopesForFile(file: KtFile, existingImports: ImportingScope? = null, createDefaultImportingScopes: Boolean = true): FileScopes {
        val packageView = components.moduleDescriptor.getPackage(file.packageFqName)
        val packageFragment = topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file)

        return FilesScopesBuilder(file, existingImports, packageFragment, packageView, createDefaultImportingScopes).result
    }

    private data class DefaultImportResolvers(
        val explicit: LazyImportResolver<KtImportInfo>,
        val allUnder: LazyImportResolver<KtImportInfo>,
        val lowPriority: LazyImportResolver<KtImportInfo>
    )

    private fun createDefaultImportResolvers(
        extraImports: Collection<KtImportInfo>,
        aliasImportNames: Collection<FqName>
    ): DefaultImportResolvers {
        val tempTrace = TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve", false)
        val allImplicitImports = defaultImports concat extraImports

        val defaultImportsFiltered = if (aliasImportNames.isEmpty()) { // optimization
            allImplicitImports
        } else {
            allImplicitImports.filter { it.isAllUnder || it.importedFqName !in aliasImportNames }
        }

        val explicit = createDefaultImportResolver(
            makeExplicitImportsIndexed(defaultImportsFiltered, components.storageManager),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames
        )
        val allUnder = createDefaultImportResolver(
            makeAllUnderImportsIndexed(defaultImportsFiltered),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames,
            excludedImports = analyzerServices.excludedImports
        )
        val lowPriority = createDefaultImportResolver(
            makeAllUnderImportsIndexed(defaultLowPriorityImports.also { imports ->
                assert(imports.all { it.isAllUnder }) { "All low priority imports must be all-under: $imports" }
            }),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames
        )

        return DefaultImportResolvers(explicit, allUnder, lowPriority)
    }

    private val defaultImportResolvers by components.storageManager.createLazyValue {
        createDefaultImportResolvers(emptyList(), emptyList())
    }

    private fun createDefaultImportResolver(
        indexedImports: IndexedImports<KtImportInfo>,
        trace: BindingTrace,
        aliasImportNames: Collection<FqName>,
        packageFragment: PackageFragmentDescriptor?,
        excludedImports: List<FqName>? = null
    ) = LazyImportResolver(
        components, indexedImports, aliasImportNames concat excludedImports, trace, packageFragment
    )

    private fun createImportResolver(
        indexedImports: IndexedImports<KtImportDirective>,
        trace: BindingTrace,
        aliasImportNames: Collection<FqName>,
        packageFragment: PackageFragmentDescriptor?,
        excludedImports: List<FqName>? = null
    ) = LazyImportResolverForKtImportDirective(
        components, indexedImports, aliasImportNames concat excludedImports, trace, packageFragment
    )

    private inner class FilesScopesBuilder(
        private val file: KtFile,
        private val existingImports: ImportingScope?,
        private val packageFragment: PackageFragmentDescriptor,
        private val packageView: PackageViewDescriptor,
        private val createDefaultImportingScopes: Boolean,
    ) {
        val imports = file.importDirectives
        val aliasImportNames = imports.mapNotNull { if (it.aliasName != null) it.importedFqName else null }

        val explicitImportResolver =
            createImportResolver(
                makeExplicitImportsIndexed(imports, components.storageManager),
                bindingTrace, aliasImportNames, packageFragment
            )
        val allUnderImportResolver = createImportResolver(
            makeAllUnderImportsIndexed(imports),
            bindingTrace,
            aliasImportNames,
            packageFragment
        ) // TODO: should we count excludedImports here also?

        val lazyImportingScope = object : ImportingScope by ImportingScope.Empty {
            // avoid constructing the scope before we query it
            override val parent: ImportingScope by components.storageManager.createLazyValue {
                createImportingScope()
            }
        }

        val lexicalScope =
            LexicalScope.Base(lazyImportingScope, topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file))

        val importResolver = object : ImportForceResolver {
            override fun forceResolveNonDefaultImports() {
                explicitImportResolver.forceResolveNonDefaultImports()
                allUnderImportResolver.forceResolveNonDefaultImports()
            }

            override fun forceResolveImport(importDirective: KtImportDirective) {
                if (importDirective.isAllUnder) {
                    allUnderImportResolver.forceResolveImport(importDirective)
                } else {
                    explicitImportResolver.forceResolveImport(importDirective)
                }
            }
        }

        val result = FileScopes(lexicalScope, lazyImportingScope, importResolver)

        private fun createDefaultImportResolversForFile(): DefaultImportResolvers {
            val extraImports = ExtraImportsProviderExtension.getInstance(file.project).getExtraImports(file)

            if (extraImports.isEmpty() && aliasImportNames.isEmpty()) {
                return defaultImportResolvers
            }

            return createDefaultImportResolvers(extraImports, aliasImportNames)
        }

        fun createImportingScope(): LazyImportScope {
            val (defaultExplicitImportResolver, defaultAllUnderImportResolver, defaultLowPriorityImportResolver) =
                    createDefaultImportResolversForFile()

            val dummyContainerDescriptor = DummyContainerDescriptor(file, packageFragment)

            var scope: ImportingScope? = existingImports

            val debugName = "LazyFileScope for file " + file.name

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope, defaultAllUnderImportResolver, defaultLowPriorityImportResolver,
                    LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                    "Default all under imports in $debugName (invisible classes only)"
                )
            }

            scope = LazyImportScope(
                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "All under imports in $debugName (invisible classes only)"
            )

            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.INVISIBLE_CLASSES, scope)

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope, defaultAllUnderImportResolver, defaultLowPriorityImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                    "Default all under imports in $debugName (visible classes)"
                )
            }

            scope = LazyImportScope(
                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "All under imports in $debugName (visible classes)"
            )

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope, defaultExplicitImportResolver, null, LazyImportScope.FilteringKind.ALL,
                    "Default explicit imports in $debugName"
                )
            }

            scope = SubpackagesImportingScope(scope, components.moduleDescriptor, FqName.ROOT)

            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.VISIBLE_CLASSES, scope)

            return LazyImportScope(scope, explicitImportResolver, null, LazyImportScope.FilteringKind.ALL, "Explicit imports in $debugName")
        }

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
        val names by lazy(LazyThreadSafetyMode.PUBLICATION) { scope.computeAllNames()?.let(::THashSet) }
        val packageName = packageView.fqName
        val excludedNames = aliasImportNames.mapNotNull { if (it.parent() == packageName) it.shortName() else null }

        return object : ImportingScope {
            override val parent: ImportingScope? = parentScope

            override fun getContributedPackage(name: Name): Nothing? = null

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

            override fun computeImportedNames() = packageView.memberScope.computeAllNames()

            override fun definitelyDoesNotContainName(name: Name) = names?.let { name !in it } == true

            override fun toString() = "Scope for current package (${filteringKind.name})"

            override fun printStructure(p: Printer) {
                p.println(this.toString())
            }
        }
    }

    // we use this dummy implementation of DeclarationDescriptor to check accessibility of symbols from the current package
    private class DummyContainerDescriptor(file: KtFile, private val packageFragment: PackageFragmentDescriptor) :
        DeclarationDescriptorNonRoot {
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

private infix fun <T> Collection<T>.concat(other: Collection<T>?) =
    if (other == null || other.isEmpty()) this else this + other
