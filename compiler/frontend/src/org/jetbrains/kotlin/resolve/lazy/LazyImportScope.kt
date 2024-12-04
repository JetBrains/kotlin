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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMapper
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilityUtils.isVisibleIgnoringReceiver
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_NATIVE_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.flatMapToNullable
import org.jetbrains.kotlin.utils.ifEmpty

open class IndexedImports<I : KtImportInfo>(val imports: Array<I>) {
    open fun importsForName(name: Name): Iterable<I> = imports.asIterable()
}

inline fun <reified I : KtImportInfo> makeAllUnderImportsIndexed(imports: Collection<I>) : IndexedImports<I> =
    IndexedImports(imports.filter { it.isAllUnder }.toTypedArray())


class ExplicitImportsIndexed<I : KtImportInfo>(
    imports: Array<I>,
    storageManager: StorageManager
) : IndexedImports<I>(imports) {

    private val nameToDirectives: NotNullLazyValue<ListMultimap<Name, I>> = storageManager.createLazyValue {
        val builder = ImmutableListMultimap.builder<Name, I>()

        for (directive in imports) {
            val importedName = directive.importedName ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives().get(name)
}

inline fun <reified I : KtImportInfo> makeExplicitImportsIndexed(
    imports: Collection<I>,
    storageManager: StorageManager
) : IndexedImports<I> =
    ExplicitImportsIndexed(imports.filter { !it.isAllUnder }.toTypedArray(), storageManager)

interface ImportForceResolver {
    fun forceResolveNonDefaultImports()
    fun forceResolveImport(importDirective: KtImportDirective)
}

class ImportResolutionComponents(
    val storageManager: StorageManager,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val moduleDescriptor: ModuleDescriptor,
    val platformToKotlinClassMapper: PlatformToKotlinClassMapper,
    val languageVersionSettings: LanguageVersionSettings,
    val deprecationResolver: DeprecationResolver,
    val optimizingOptions: OptimizingOptions,
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

    fun <D : DeclarationDescriptor> collectFromImports(name: Name, descriptorsSelector: (ImportingScope) -> Collection<D>): Collection<D> =
        components.storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                val descriptorsForImport = descriptorsSelector(getImportScope(directive))
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors.orEmpty()
        }

    fun getImportScope(directive: KtImportInfo): ImportingScope {
        return importedScopesProvider(directive) ?: ImportingScope.Empty
    }

    val allNames: Set<Name>? by components.storageManager.createNullableLazyValue {
        indexedImports.imports.asIterable().flatMapToNullable(ObjectOpenHashSet()) { getImportScope(it).computeImportedNames() }
    }

    fun definitelyDoesNotContainName(name: Name): Boolean {
        // Calculation of all names is undesirable for cases when the scope doesn't live long and is big enough.
        // In such cases we often do the same work twice - first time for computing definitelyDoesNotContainName
        // and second time for resolution itself. Results seem to be not reused.
        // This optimization is used in Kotlin Notebooks
        return if (components.optimizingOptions.shouldCalculateAllNamesForLazyImportScopeOptimizing(packageFragment?.containingDeclaration)) {
            allNames?.let { name !in it } == true
        } else {
            false
        }
    }

    fun recordLookup(name: Name, location: LookupLocation) {
        if (allNames == null) return
        for (it in indexedImports.importsForName(name)) {
            val scope = getImportScope(it)
            if (scope !== ImportingScope.Empty) {
                scope.recordLookup(name, location)
            }
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
                components.platformToKotlinClassMapper, traceForImportResolve, directive, allDescriptors
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
    private val filteringKind: FilteringKind,
    private val debugName: String
) : ImportingScope {

    enum class FilteringKind {
        ALL,
        VISIBLE_CLASSES,
        INVISIBLE_CLASSES
    }

    private fun LazyImportResolver<*>.isClassifierVisible(descriptor: ClassifierDescriptor): Boolean {
        if (filteringKind == FilteringKind.ALL) return true

        // TODO: do not perform this check here because for correct work it requires corresponding PSI element
        if (components.deprecationResolver.isHiddenInResolution(descriptor, fromImportingScope = true)) return false

        val visibility = (descriptor as DeclarationDescriptorWithVisibility).visibility
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        if (!visibility.mustCheckInImports()) return includeVisible
        val fromDescriptor =
            if (components.languageVersionSettings.supportsFeature(LanguageFeature.ProperInternalVisibilityCheckInImportingScope)) {
                packageFragment ?: components.moduleDescriptor
            } else {
                components.moduleDescriptor
            }
        return isVisibleIgnoringReceiver(
            descriptor, fromDescriptor, components.languageVersionSettings
        ) == includeVisible
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return importResolver.getClassifier(name, location) ?: secondaryImportResolver?.getClassifier(name, location)
    }

    private fun LazyImportResolver<*>.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        components.storageManager.compute {
            val imports = indexedImports.importsForName(name)

            var target: ClassifierDescriptor? = null
            for (directive in imports) {
                val descriptor = getImportScope(directive).getContributedClassifier(name, location)
                if (descriptor !is ClassDescriptor && descriptor !is TypeAliasDescriptor || !isClassifierVisible(descriptor))
                    continue /* type parameters can't be imported */
                if (target != null && target != descriptor) {
                    if (isKotlinOrJvmThrowsAmbiguity(descriptor, target) || isKotlinOrNativeThrowsAmbiguity(descriptor, target)) {
                        if (descriptor.isKotlinThrows()) {
                            target = descriptor
                        }
                    } else {
                        return@compute null // ambiguity
                    }
                } else {
                    target = descriptor
                }
            }

            target
        }

    private fun isKotlinOrJvmThrowsAmbiguity(c1: ClassifierDescriptor, c2: ClassifierDescriptor) =
        c1.isKotlinOrJvmThrows() && c2.isKotlinOrJvmThrows()

    private fun isKotlinOrNativeThrowsAmbiguity(c1: ClassifierDescriptor, c2: ClassifierDescriptor) =
        c1.isKotlinOrNativeThrows() && c2.isKotlinOrNativeThrows()

    private fun ClassifierDescriptor.isKotlinThrows() = fqNameOrNull() == KOTLIN_THROWS_ANNOTATION_FQ_NAME
    private fun ClassifierDescriptor.isKotlinOrJvmThrows(): Boolean {
        if (name != JVM_THROWS_ANNOTATION_FQ_NAME.shortName()) return false
        return isKotlinThrows() || fqNameOrNull() == JVM_THROWS_ANNOTATION_FQ_NAME
    }

    private fun ClassifierDescriptor.isKotlinOrNativeThrows(): Boolean {
        if (name != KOTLIN_THROWS_ANNOTATION_FQ_NAME.shortName()) return false
        return isKotlinThrows() || fqNameOrNull() == KOTLIN_NATIVE_THROWS_ANNOTATION_FQ_NAME
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope -> scope.getContributedVariables(name, location) }.ifEmpty {
            secondaryImportResolver?.collectFromImports(name) { scope -> scope.getContributedVariables(name, location) }.orEmpty()
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope -> scope.getContributedFunctions(name, location) }.ifEmpty {
            secondaryImportResolver?.collectFromImports(name) { scope -> scope.getContributedFunctions(name, location) }.orEmpty()
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
