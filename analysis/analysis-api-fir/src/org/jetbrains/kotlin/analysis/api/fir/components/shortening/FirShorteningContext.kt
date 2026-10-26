/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.shortening

import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.fir.utils.getAvailableScopesForPosition
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * How a symbol is imported. The order of the enum entry represents the priority of imports. If a symbol is available from multiple kinds of
 * imports, the symbol from "smaller" kind is used. For example, an explicitly imported symbol can overwrite a star-imported symbol.
 */
internal enum class ShorteningImportKind {
    /** The symbol is available from the local scope and hence cannot be imported or overwritten. */
    LOCAL,

    /** Explicitly imported by user. */
    EXPLICIT,

    /** Implicitly imported from package. */
    PACKAGE,

    /** Explicitly imported by Kotlin default. For example, `kotlin.String`. */
    DEFAULT_EXPLICIT,

    /** Star imported (star import) by user. */
    STAR,

    /** Star imported (star import) by Kotlin default. */
    DEFAULT_STAR;

    fun hasHigherPriorityThan(that: ShorteningImportKind): Boolean = this < that

    companion object {
        fun fromScope(scope: FirScope): ShorteningImportKind {
            return when (scope) {
                is FirDefaultStarImportingScope -> DEFAULT_STAR
                is FirAbstractStarImportingScope -> STAR
                is FirPackageMemberScope -> PACKAGE
                is FirDefaultSimpleImportingScope -> DEFAULT_EXPLICIT
                is FirAbstractSimpleImportingScope -> EXPLICIT
                else -> LOCAL
            }
        }

        fun fromShortenOption(option: ShortenStrategy): ShorteningImportKind? = when (option) {
            ShortenStrategy.SHORTEN_AND_IMPORT -> EXPLICIT
            ShortenStrategy.SHORTEN_AND_STAR_IMPORT -> STAR
            else -> null
        }
    }
}

internal data class ShorteningAvailableSymbol<out T>(
    val symbol: T,
    val importKind: ShorteningImportKind,
)

internal class FirShorteningContext(val analysisSession: KaFirSession) {
    private val resolutionFacade = analysisSession.resolutionFacade

    private val firSession: FirSession
        get() = resolutionFacade.useSiteFirSession

    class ClassifierCandidate(val scope: FirScope, val availableSymbol: ShorteningAvailableSymbol<FirClassifierSymbol<*>>)

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): ShorteningAvailableSymbol<FirClassifierSymbol<*>>? =
        positionScopes.firstNotNullOfOrNull { scope -> findFirstClassifierSymbolByName(scope, targetClassName) }

    fun findClassifiersInScopesByName(scopes: List<FirScope>, targetClassName: Name): List<ClassifierCandidate> =
        scopes.mapNotNull { scope ->
            val classifierSymbol = findFirstClassifierSymbolByName(scope, targetClassName) ?: return@mapNotNull null

            ClassifierCandidate(scope, classifierSymbol)
        }

    private fun findFirstClassifierSymbolByName(scope: FirScope, targetClassName: Name): ShorteningAvailableSymbol<FirClassifierSymbol<*>>? {
        val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: return null

        return ShorteningAvailableSymbol(classifierSymbol, ShorteningImportKind.fromScope(scope))
    }

    private fun FirClassLikeSymbol<*>.getSamConstructor(): FirNamedFunctionSymbol? {
        val samResolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))

        return samResolver.getSamConstructor(fir)?.symbol
    }

    /**
     * Finds constructors with a given [targetClassName] available within the [scope], including SAM constructors
     * (which are not explicitly declared in the class).
     *
     * Includes type-aliased constructors too if typealias confirms to the [targetClassName].
     *
     * Do not confuse with constructors **declared** in the scope (see [FirScope.processDeclaredConstructors]).
     */
    private fun findAvailableConstructors(scope: FirScope, targetClassName: Name): List<FirFunctionSymbol<*>> {
        val classLikeSymbol = scope.findFirstClassifierByName(targetClassName) as? FirClassLikeSymbol
            ?: return emptyList()

        @OptIn(DirectDeclarationsAccess::class)
        val constructors = (classLikeSymbol as? FirClassSymbol)?.declarationSymbols?.filterIsInstance<FirConstructorSymbol>().orEmpty()
        val samConstructor = classLikeSymbol.getSamConstructor()

        return constructors + listOfNotNull(samConstructor)
    }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<ShorteningAvailableSymbol<FirFunctionSymbol<*>>> {
        return scopes.flatMap { scope ->
            val importKind = ShorteningImportKind.fromScope(scope)
            buildList {
                // Collect constructors
                findAvailableConstructors(scope, name).mapTo(this) { ShorteningAvailableSymbol(it, importKind) }

                // Collect functions
                scope.getFunctions(name).mapTo(this) { ShorteningAvailableSymbol(it, importKind) }
            }
        }
    }

    fun findPropertiesInScopes(scopes: List<FirScope>, name: Name): List<ShorteningAvailableSymbol<FirVariableSymbol<*>>> {
        return scopes.flatMap { scope ->
            val importKind = ShorteningImportKind.fromScope(scope)
            scope.getProperties(name).map {
                ShorteningAvailableSymbol(it, importKind)
            }
        }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    fun findScopesAtPosition(
        position: KtElement,
        newImports: Sequence<FqName>,
        towerContextProvider: ShorteningTowerDataContextProvider,
        withImplicitReceivers: Boolean = true,
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements
            .asSequence()
            .filter { withImplicitReceivers || it.implicitReceiver == null }
            .flatMap {
                // We must use `it.getAvailableScopesForPosition(position)` instead of `it.scope` to check scopes of companion objects
                // and context receivers as well.
                it.getAvailableScopesForPosition(position)
            }

        val result = buildList {
            addAll(nonLocalScopes)
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: Sequence<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }.toList()
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(
            resolvedNewImports,
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
    }

    private fun createFakeResolvedImport(fqNameToImport: FqName): FirResolvedImport? {
        val packageOrClass =
            (resolveToPackageOrClass(firSession.symbolProvider, fqNameToImport) as? PackageResolutionResult.PackageOrClass) ?: return null

        val delegateImport = buildImport {
            importedFqName = fqNameToImport
            isAllUnder = false
        }

        return buildResolvedImport {
            delegate = delegateImport
            packageFqName = packageOrClass.packageFqName
        }
    }

    fun getRegularClass(type: ConeKotlinType?): FirRegularClass? {
        return type?.toRegularClassSymbol(firSession)?.fir
    }

    fun toClassSymbol(classId: ClassId) =
        firSession.symbolProvider.getClassLikeSymbolByClassId(classId)

    fun convertToImportableName(callableSymbol: FirCallableSymbol<*>): FqName? =
        callableSymbol.computeImportableName()
}
