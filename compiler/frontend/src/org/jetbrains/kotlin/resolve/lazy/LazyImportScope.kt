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

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.PlatformClassesMappedToKotlinChecker
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.isHiddenInResolution
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import java.util.*

interface IndexedImports {
    val imports: List<KtImportDirective>
    fun importsForName(name: Name): Collection<KtImportDirective>
}

class AllUnderImportsIndexed(allImports: Collection<KtImportDirective>) : IndexedImports {
    override val imports = allImports.filter { it.isAllUnder() }
    override fun importsForName(name: Name) = imports
}

class ExplicitImportsIndexed(allImports: Collection<KtImportDirective>) : IndexedImports {
    override val imports = allImports.filter { !it.isAllUnder() }

    private val nameToDirectives: ListMultimap<Name, KtImportDirective> by lazy {
        val builder = ImmutableListMultimap.builder<Name, KtImportDirective>()

        for (directive in imports) {
            val path = directive.getImportPath() ?: continue // parse error
            val importedName = path.getImportedName() ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives.get(name)
}

interface ImportResolver {
    fun forceResolveAllImports()
    fun forceResolveImport(importDirective: KtImportDirective)
}

class LazyImportResolver(
        val storageManager: StorageManager,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        val moduleDescriptor: ModuleDescriptor,
        private val platformToKotlinClassMap: PlatformToKotlinClassMap,
        val languageVersionSettings: LanguageVersionSettings,
        val indexedImports: IndexedImports,
        excludedImportNames: Collection<FqName>,
        private val traceForImportResolve: BindingTrace,
        private val packageFragment: PackageFragmentDescriptor
) : ImportResolver {
    private val importedScopesProvider = storageManager.createMemoizedFunctionWithNullableValues {
        directive: KtImportDirective ->

        qualifiedExpressionResolver.processImportReference(
                directive, moduleDescriptor, traceForImportResolve, excludedImportNames, packageFragment
        )?.apply {
            if (!directive.isAllUnder) {
                PlatformClassesMappedToKotlinChecker.checkPlatformClassesMappedToKotlin(
                        platformToKotlinClassMap, traceForImportResolve, directive, getContributedDescriptors()
                )
            }
        }
    }

    override fun forceResolveAllImports() {
        val explicitClassImports = HashMultimap.create<String, KtImportDirective>()
        for (importDirective in indexedImports.imports) {
            forceResolveImport(importDirective)
            val scope = importedScopesProvider(importDirective)

            val alias = KtPsiUtil.getAliasName(importDirective)?.identifier
            if (scope != null && alias != null) {
                if (scope.getContributedClassifier(Name.identifier(alias), KotlinLookupLocation(importDirective)) != null) {
                    explicitClassImports.put(alias, importDirective)
                }
            }
        }
        for ((alias, import) in explicitClassImports.entries()) {
            if (alias.all { it == '_' }) {
                traceForImportResolve.report(Errors.UNDERSCORE_IS_RESERVED.on(import))
            }
        }
        for (alias in explicitClassImports.keySet()) {
            val imports = explicitClassImports.get(alias)
            if (imports.size > 1) {
                imports.forEach {
                    traceForImportResolve.report(Errors.CONFLICTING_IMPORT.on(it, alias))
                }
            }
        }
    }

    override fun forceResolveImport(importDirective: KtImportDirective) {
        getImportScope(importDirective)
    }

    fun <D : DeclarationDescriptor> selectSingleFromImports(
            name: Name,
            descriptorSelector: (ImportingScope, Name) -> D?
    ): D? {
        fun compute(): D? {
            val imports = indexedImports.importsForName(name)

            var target: D? = null
            for (directive in imports) {
                val resolved = descriptorSelector(getImportScope(directive), name) ?: continue
                if (target != null && target != resolved) return null // ambiguity
                target = resolved
            }
            return target
        }
        return storageManager.compute(::compute)
    }

    fun <D : DeclarationDescriptor> collectFromImports(
            name: Name,
            descriptorsSelector: (ImportingScope, Name) -> Collection<D>
    ): Collection<D> {
        return storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                val descriptorsForImport = descriptorsSelector(getImportScope(directive), name)
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors ?: emptySet<D>()
        }
    }

    fun getImportScope(directive: KtImportDirective): ImportingScope {
        return importedScopesProvider(directive) ?: ImportingScope.Empty
    }
}

class LazyImportScope(
        override val parent: ImportingScope?,
        private val importResolver: LazyImportResolver,
        private val filteringKind: LazyImportScope.FilteringKind,
        private val debugName: String
) : ImportingScope {

    enum class FilteringKind {
        ALL,
        VISIBLE_CLASSES,
        INVISIBLE_CLASSES
    }

    private fun isClassifierVisible(descriptor: ClassifierDescriptor): Boolean {
        if (filteringKind == FilteringKind.ALL) return true

        if (descriptor.isHiddenInResolution(importResolver.languageVersionSettings)) return false

        val visibility = (descriptor as DeclarationDescriptorWithVisibility).visibility
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        if (!visibility.mustCheckInImports()) return includeVisible
        return Visibilities.isVisibleIgnoringReceiver(descriptor, importResolver.moduleDescriptor) == includeVisible
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return importResolver.selectSingleFromImports(name) { scope, name ->
            val descriptor = scope.getContributedClassifier(name, location)
            if ((descriptor is ClassDescriptor || descriptor is TypeAliasDescriptor) && isClassifierVisible(descriptor))
                descriptor
            else
                null /* type parameters can't be imported */
        }
    }

    override fun getContributedPackage(name: Name) = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getContributedVariables(name, location) }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getContributedFunctions(name, location) }
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            val descriptors = LinkedHashSet<DeclarationDescriptor>()
            for (directive in importResolver.indexedImports.imports) {
                val importPath = directive.getImportPath() ?: continue
                val importedName = importPath.getImportedName()
                if (importedName == null || nameFilter(importedName)) {
                    descriptors.addAll(importResolver.getImportScope(directive).getContributedDescriptors(kindFilter, nameFilter))
                }
            }
            descriptors
        }
    }

    override fun toString() = "LazyImportScope: " + debugName

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }
}
