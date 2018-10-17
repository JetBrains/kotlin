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
import gnu.trove.THashSet
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.flatMapToNullable
import org.jetbrains.kotlin.utils.ifEmpty

interface IndexedImports<I : KtImportInfo> {
    val imports: List<I>
    fun importsForName(name: Name): Collection<I>
}

class AllUnderImportsIndexed<I : KtImportInfo>(allImports: Collection<I>) : IndexedImports<I> {
    override val imports = allImports.filter { it.isAllUnder }
    override fun importsForName(name: Name) = imports
}

class ExplicitImportsIndexed<I : KtImportInfo>(allImports: Collection<I>) : IndexedImports<I> {
    override val imports = allImports.filter { !it.isAllUnder }

    private val nameToDirectives: ListMultimap<Name, I> by lazy {
        val builder = ImmutableListMultimap.builder<Name, I>()

        for (directive in imports) {
            val importedName = directive.importedName ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives.get(name)
}

interface ImportForceResolver {
    fun forceResolveNonDefaultImports()
    fun forceResolveImport(importDirective: KtImportDirective)
}

class ImportResolutionComponents(
    val storageManager: StorageManager,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val moduleDescriptor: ModuleDescriptor,
    val platformToKotlinClassMap: PlatformToKotlinClassMap,
    val languageVersionSettings: LanguageVersionSettings,
    val deprecationResolver: DeprecationResolver
)

open class LazyImportResolver<I : KtImportInfo>(
    internal val components: ImportResolutionComponents,
    val indexedImports: IndexedImports<I>,
    val excludedImportNames: Collection<FqName>,
    val traceForImportResolve: BindingTrace,
    val packageFragment: PackageFragmentDescriptor?
) {
    private val importedScopesProvider = with(components) {
        storageManager.createMemoizedFunctionWithNullableValues { directive: KtImportInfo ->
            qualifiedExpressionResolver.processImportReference(
                directive, moduleDescriptor, traceForImportResolve, excludedImportNames, packageFragment
            )
        }
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
        return components.storageManager.compute(::compute)
    }

    fun <D : DeclarationDescriptor> collectFromImports(
        name: Name,
        descriptorsSelector: (ImportingScope, Name) -> Collection<D>
    ): Collection<D> {
        return components.storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                val descriptorsForImport = descriptorsSelector(getImportScope(directive), name)
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors ?: emptySet<D>()
        }
    }

    fun getImportScope(directive: KtImportInfo): ImportingScope {
        return importedScopesProvider(directive) ?: ImportingScope.Empty
    }

    val allNames: Set<Name>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        indexedImports.imports.flatMapToNullable(THashSet()) { getImportScope(it).computeImportedNames() }
    }

    fun definitelyDoesNotContainName(name: Name) = allNames?.let { name !in it } == true

    fun recordLookup(name: Name, location: LookupLocation) {
        if (allNames == null) return
        indexedImports.importsForName(name).forEach {
            getImportScope(it).recordLookup(name, location)
        }
    }
}

class LazyImportResolverForKtImportDirective(
    components: ImportResolutionComponents,
    indexedImports: IndexedImports<KtImportDirective>,
    excludedImportNames: Collection<FqName>,
    traceForImportResolve: BindingTrace,
    packageFragment: PackageFragmentDescriptor?
) : LazyImportResolver<KtImportDirective>(
    components, indexedImports, excludedImportNames, traceForImportResolve, packageFragment
), ImportForceResolver {

    private val forceResolveImportDirective = components.storageManager.createMemoizedFunction { directive: KtImportDirective ->
        val scope = getImportScope(directive)
        if (scope is LazyExplicitImportScope) {
            val allDescriptors = scope.storeReferencesToDescriptors()
            PlatformClassesMappedToKotlinChecker.checkPlatformClassesMappedToKotlin(
                components.platformToKotlinClassMap, traceForImportResolve, directive, allDescriptors
            )
        }

        Unit
    }

    private val forceResolveNonDefaultImportsTask: NotNullLazyValue<Unit> = components.storageManager.createLazyValue {
        val explicitClassImports = HashMultimap.create<String, KtImportDirective>()
        for (importInfo in indexedImports.imports) {
            forceResolveImport(importInfo)

            val scope = getImportScope(importInfo)

            val alias = importInfo.importedName
            if (alias != null) {
                val lookupLocation = KotlinLookupLocation(importInfo)
                if (scope.getContributedClassifier(alias, lookupLocation) != null) {
                    explicitClassImports.put(alias.asString(), importInfo)
                }
            }

            checkResolvedImportDirective(importInfo)
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

    override fun forceResolveNonDefaultImports() {
        forceResolveNonDefaultImportsTask()
    }

    private fun checkResolvedImportDirective(importDirective: KtImportInfo) {
        if (importDirective !is KtImportDirective) return
        val importedReference = KtPsiUtil.getLastReference(importDirective.importedReference ?: return) ?: return
        val importedDescriptor = traceForImportResolve.bindingContext.get(BindingContext.REFERENCE_TARGET, importedReference) ?: return

        val aliasName = importDirective.aliasName

        if (importedDescriptor is FunctionDescriptor && importedDescriptor.isOperator &&
            aliasName != null && OperatorConventions.isConventionName(Name.identifier(aliasName))) {
            traceForImportResolve.report(Errors.OPERATOR_RENAMED_ON_IMPORT.on(importedReference))
        }
    }

    override fun forceResolveImport(importDirective: KtImportDirective) {
        forceResolveImportDirective(importDirective)
    }
}

class LazyImportScope(
    override val parent: ImportingScope?,
    private val importResolver: LazyImportResolver<*>,
    private val secondaryImportResolver: LazyImportResolver<*>?,
    private val filteringKind: LazyImportScope.FilteringKind,
    private val debugName: String
) : ImportingScope {

    enum class FilteringKind {
        ALL,
        VISIBLE_CLASSES,
        INVISIBLE_CLASSES
    }

    private fun LazyImportResolver<*>.isClassifierVisible(descriptor: ClassifierDescriptor): Boolean {
        if (filteringKind == FilteringKind.ALL) return true

        if (components.deprecationResolver.isHiddenInResolution(descriptor)) return false

        val visibility = (descriptor as DeclarationDescriptorWithVisibility).visibility
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        if (!visibility.mustCheckInImports()) return includeVisible
        return Visibilities.isVisibleIgnoringReceiver(descriptor, components.moduleDescriptor) == includeVisible
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return importResolver.getClassifier(name, location) ?: secondaryImportResolver?.getClassifier(name, location)
    }

    private fun LazyImportResolver<*>.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return selectSingleFromImports(name) { scope, _ ->
            val descriptor = scope.getContributedClassifier(name, location)
            if ((descriptor is ClassDescriptor || descriptor is TypeAliasDescriptor) && isClassifierVisible(descriptor))
                descriptor
            else
                null /* type parameters can't be imported */
        }
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, _ -> scope.getContributedVariables(name, location) }.ifEmpty {
            secondaryImportResolver?.collectFromImports(name) { scope, _ -> scope.getContributedVariables(name, location) }.orEmpty()
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, _ -> scope.getContributedFunctions(name, location) }.ifEmpty {
            secondaryImportResolver?.collectFromImports(name) { scope, _ -> scope.getContributedFunctions(name, location) }.orEmpty()
        }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        val storageManager = importResolver.components.storageManager
        if (secondaryImportResolver != null) {
            assert(storageManager === secondaryImportResolver.components.storageManager) { "Multiple storage managers are not supported" }
        }

        return storageManager.compute {
            val result = linkedSetOf<DeclarationDescriptor>()
            val importedNames = if (secondaryImportResolver == null) null else hashSetOf<Name>()

            for (directive in importResolver.indexedImports.imports) {
                val importedName = directive.importedName
                if (importedName == null || nameFilter(importedName)) {
                    val newDescriptors =
                        importResolver.getImportScope(directive).getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
                    result.addAll(newDescriptors)

                    if (importedNames != null) {
                        for (descriptor in newDescriptors) {
                            importedNames.add(descriptor.name)
                        }
                    }
                }
            }

            secondaryImportResolver?.let { resolver ->
                for (directive in resolver.indexedImports.imports) {
                    val newDescriptors =
                        resolver.getImportScope(directive).getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)

                    for (descriptor in newDescriptors) {
                        if (descriptor.name !in importedNames!!) {
                            result.add(descriptor)
                        }
                    }
                }
            }

            result
        }
    }

    override fun toString() = "LazyImportScope: $debugName"

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean =
        importResolver.definitelyDoesNotContainName(name) && secondaryImportResolver?.definitelyDoesNotContainName(name) != false

    override fun recordLookup(name: Name, location: LookupLocation) {
        importResolver.recordLookup(name, location)
        secondaryImportResolver?.recordLookup(name, location)
    }

    override fun computeImportedNames(): Set<Name>? = importResolver.allNames?.union(secondaryImportResolver?.allNames.orEmpty())
}
