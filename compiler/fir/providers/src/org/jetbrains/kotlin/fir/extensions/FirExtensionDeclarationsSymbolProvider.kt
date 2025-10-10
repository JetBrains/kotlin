/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

@OptIn(FirExtensionApiInternals::class, ExperimentalTopLevelDeclarationsGenerationApi::class)
class FirExtensionDeclarationsSymbolProvider private constructor(
    session: FirSession,
    cachesFactory: FirCachesFactory,
    private val extensions: List<FirDeclarationGenerationExtension>
) : FirSymbolProvider(session), FirSessionComponent {
    companion object {
        fun createIfNeeded(session: FirSession): FirExtensionDeclarationsSymbolProvider? {
            val extensions = session.extensionService.declarationGenerators
            if (extensions.isEmpty()) return null
            return FirExtensionDeclarationsSymbolProvider(session, session.firCachesFactory, extensions)
        }
    }

    // ------------------------------------------ caches ------------------------------------------

    private val classCache: FirCache<ClassId, FirClassLikeSymbol<*>?, Nothing?> = cachesFactory.createCache { classId, _ ->
        generateClassLikeDeclaration(classId)
    }

    private val functionCache: FirCache<CallableId, List<FirNamedFunctionSymbol>, Nothing?> = cachesFactory.createCache { callableId, _ ->
        generateTopLevelFunctions(callableId)
    }

    private val propertyCache: FirCache<CallableId, List<FirPropertySymbol>, Nothing?> = cachesFactory.createCache { callableId, _ ->
        generateTopLevelProperties(callableId)
    }

    private val packageCache: FirCache<FqName, Boolean, Nothing?> = cachesFactory.createCache { packageFqName, _ ->
        extensions.any { it.hasPackage(packageFqName) }
    }

    private val callableNamesInPackageCache: FirLazyValue<Map<FqName, Set<Name>>> =
        cachesFactory.createLazyValue {
            computeNamesGroupedByPackage(
                FirDeclarationGenerationExtension::getTopLevelCallableIds,
                CallableId::packageName, CallableId::callableName
            )
        }

    private val classNamesInPackageCache: FirLazyValue<Map<FqName, Set<Name>>> =
        cachesFactory.createLazyValue {
            computeNamesGroupedByPackage(
                FirDeclarationGenerationExtension::getTopLevelClassIds,
                ClassId::packageFqName,
                ClassId::shortClassName,
            )
        }

    private fun <I, N> computeNamesGroupedByPackage(
        ids: FirDeclarationGenerationExtension.() -> Collection<I>,
        packageFqName: (I) -> FqName,
        shortName: (I) -> N,
    ): Map<FqName, Set<N>> =
        buildMap<FqName, MutableSet<N>> {
            for (extension in extensions) {
                for (id in extension.ids()) {
                    getOrPut(packageFqName(id)) { mutableSetOf() }.add(shortName(id))
                }
            }
        }

    private val extensionsByTopLevelClassId: FirLazyValue<Map<ClassId, List<FirDeclarationGenerationExtension>>> =
        session.firCachesFactory.createLazyValue {
            extensions.flatGroupBy { it.topLevelClassIdsCache.getValue() }
        }

    private val extensionsByTopLevelCallableId: FirLazyValue<Map<CallableId, List<FirDeclarationGenerationExtension>>> =
        session.firCachesFactory.createLazyValue {
            extensions.flatGroupBy { it.topLevelCallableIdsCache.getValue() }
        }

    // ------------------------------------------ generators ------------------------------------------

    private fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        return when {
            // TODO: KT-81840 consider writing require(!classId.isLocal) here or supporting this case
            @OptIn(ClassIdBasedLocality::class)
            classId.isLocal -> null

            classId.isNestedClass -> {
                // Note: session.symbolProvider is important here, as we need a full composite provider and not only this extension provider
                val owner = session.symbolProvider.getClassLikeSymbolByClassId(classId.outerClassId!!) as? FirClassSymbol<*> ?: return null
                val nestedClassifierScope = session.nestedClassifierScope(owner.fir) ?: return null
                var result: FirClassLikeSymbol<*>? = null
                nestedClassifierScope.processClassifiersByName(classId.shortClassName) {
                    if (it is FirClassLikeSymbol<*>) {
                        result = it
                    }
                }
                // Lombok plugin sets the origin of generated declarations to FirDeclarationOrigin.Java.Source.
                // TODO(KT-79778) Remove check for FirDeclarationOrigin.Java.Source when we have a proper generated Java origin.
                result?.takeIf { it.origin.generated || it.origin == FirDeclarationOrigin.Java.Source }
            }
            else -> {
                val matchedExtensions = extensionsByTopLevelClassId.getValue()[classId] ?: return null
                val generatedClasses = matchedExtensions
                    .mapNotNull { generatorExtension ->
                        generatorExtension.generateTopLevelClassLikeDeclaration(classId)?.also { symbol ->
                            symbol.fir.ownerGenerator = generatorExtension
                        }
                    }
                    .onEach { it.fir.validate() }
                when (generatedClasses.size) {
                    0 -> null
                    1 -> generatedClasses.first()
                    else -> error("Multiple plugins generated classes with same classId $classId\n${generatedClasses.joinToString("\n") { it.fir.render() }}")
                }
            }
        }
    }

    private fun generateTopLevelFunctions(callableId: CallableId): List<FirNamedFunctionSymbol> {
        return extensionsByTopLevelCallableId.getValue()[callableId].orEmpty()
            .flatMap { it.generateFunctions(callableId, context = null) }
            .onEach { it.fir.validate() }
    }

    private fun generateTopLevelProperties(callableId: CallableId): List<FirPropertySymbol> {
        return extensionsByTopLevelCallableId.getValue()[callableId].orEmpty()
            .flatMap { it.generateProperties(callableId, context = null) }
            .onEach { it.fir.validate() }
    }

    // ------------------------------------------ provider methods ------------------------------------------

    /**
     * Even though we use [FirSymbolNamesProvider.mayHaveTopLevelClassifier] and [FirSymbolNamesProvider.mayHaveTopLevelCallable] below, the
     * symbol names provider doesn't need to be cached. That's because `mayHaveTopLevel*` functions access
     * [FirSymbolNamesProvider.getTopLevelClassifierNamesInPackage] and [FirSymbolNamesProvider.getTopLevelCallableNamesInPackage] directly,
     * which in this symbol names provider are already supplied from caches.
     *
     * In contrast, package sets are computed anew on every request, but they aren't used directly by `mayHaveTopLevel*` functions.
     */
    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProvider() {
        override val hasSpecificClassifierPackageNamesComputation: Boolean get() = true

        override fun getPackageNames(): Set<String> =
            getPackageNamesWithTopLevelClassifiers() + getPackageNamesWithTopLevelCallables()

        override fun getPackageNamesWithTopLevelClassifiers(): Set<String> =
            buildSet {
                extensions.forEach { extension ->
                    extension.topLevelClassIdsCache.getValue().mapTo(this) { it.packageFqName.asString() }
                }
            }

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
            classNamesInPackageCache.getValue()[packageFqName] ?: emptySet()

        override val hasSpecificCallablePackageNamesComputation: Boolean get() = true

        override fun getPackageNamesWithTopLevelCallables(): Set<String> =
            buildSet {
                extensions.forEach { extension ->
                    extension.topLevelCallableIdsCache.getValue().mapTo(this) { it.packageName.asString() }
                }
            }

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
            callableNamesInPackageCache.getValue()[packageFqName].orEmpty()
    }

    // NOTE: We should fill the caches only when the FIR extension can generate such a declaration to avoid a large number of null/empty
    // entries, hence the use of `mayHaveTopLevel*` before accessing caches.

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        // A FIR extension may generate a nested classifier for an outer classifier provided by a different symbol provider. This deviates
        // from the normal behavior where the nested classifier and its outermost (top-level) classifier are provided by the same symbol
        // provider. So we can only check `mayHaveTopLevelClassifier` *local to this symbol provider* for top-level class ID requests. For
        // nested classifiers, we would have to call `mayHaveTopLevelClassifier` on the session symbol provider's level, but such a check
        // should rather be handled at the top level.
        //
        // Compare also the handling of nested classes in `generateClassLikeDeclaration`, where we specifically fetch the outer class symbol
        // from `session.symbolProvider`, not *this* symbol provider. There doesn't seem to be a quick check that we can perform instead, so
        // for nested classes we have to access the cache and attempt the generation.
        if (!classId.isNestedClass && !symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return classCache.getValue(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getValue(callableId)
        destination += propertyCache.getValue(callableId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        destination += functionCache.getValue(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        destination += propertyCache.getValue(CallableId(packageFqName, name))
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return packageCache.getValue(fqName, null)
    }
}
