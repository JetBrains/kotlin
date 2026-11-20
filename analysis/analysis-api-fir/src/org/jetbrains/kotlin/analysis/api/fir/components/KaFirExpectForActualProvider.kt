/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.isExpect
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatchingContextImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

context(relationProvider: KaFirSymbolRelationProvider)
internal fun computeExpectsForActual(symbol: KaDeclarationSymbol): List<KaDeclarationSymbol> {
    symbol.getExpectsForActualSpecial()?.let { return it }

    require(symbol is KaFirSymbol<*>)
    val firSymbol = symbol.firSymbol
    if (firSymbol !is FirCallableSymbol && firSymbol !is FirClassSymbol && firSymbol !is FirTypeAliasSymbol) {
        return emptyList()
    }

    val actualModule = relationProvider.containingModule(symbol)
    if (actualModule is KaLibraryModule || actualModule is KaLibrarySourceModule) {
        if (firSymbol.isExpect()) {
            /** For libraries, we can safely rely on the [isExpect] flag. */
            return emptyList()
        }

        // Dependency tree for libraries isn't available (KT-61210) so there's no way other than checking the entire library scope.
        // Notably, the current library isn't filtered as the same library may contain binary roots for both common and platform parts.
        val expectLibraryScope = ProjectScope.getLibrariesScope(project)

        val expectDeclarationProvider = KotlinDeclarationProviderFactory.getInstance(project)
            .createDeclarationProvider(expectLibraryScope, analysisSession.useSiteModule)

        val firSymbols = when (firSymbol) {
            is FirClassLikeSymbol -> computeExpectsForLibraryClass(firSymbol, actualModule, expectDeclarationProvider)
            is FirCallableSymbol -> computeExpectsForLibraryCallable(firSymbol, actualModule, expectDeclarationProvider)
            else -> emptyList()
        }

        return firSymbols.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }
    }

    return firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
        ?.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }.orEmpty()
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun KaDeclarationSymbol.getExpectsForActualSpecial(): List<KaDeclarationSymbol>? {
    val actualName = this.name

    when (this) {
        is KaReceiverParameterSymbol -> {
            return getExpectsForActualParent(relationProvider.containingDeclaration(this) as? KaCallableSymbol) { expectParent ->
                expectParent.receiverParameter
            }
        }
        is KaTypeParameterSymbol -> {
            val actualParent = relationProvider.containingDeclaration(this) ?: return emptyList()
            val actualIndex = actualParent.typeParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
            return getExpectsForActualParent(actualParent) { expectParent ->
                /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.TypeParameterNames] */
                expectParent.typeParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
            }
        }
        is KaContextParameterSymbol -> {
            val actualParent = relationProvider.containingDeclaration(this) as? KaCallableSymbol ?: return emptyList()
            val actualIndex = actualParent.contextParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
            return getExpectsForActualParent(actualParent) { expectParent ->
                /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.ContextParameterNames] */
                expectParent.contextParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
            }
        }
        is KaValueParameterSymbol -> {
            val actualParent = relationProvider.containingDeclaration(this) as? KaFunctionSymbol ?: return emptyList()
            val actualIndex = actualParent.valueParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
            return getExpectsForActualParent(actualParent) { expectParent: KaFunctionSymbol ->
                /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.ParameterNames] */
                expectParent.valueParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
            }
        }
        is KaPropertyAccessorSymbol -> {
            val isGetter = this is KaPropertyGetterSymbol
            return getExpectsForActualParent(relationProvider.containingDeclaration(this) as? KaPropertySymbol) { expectProperty ->
                if (isGetter) expectProperty.getter else expectProperty.setter
            }
        }
        else -> {
            // The given symbol isn't specially treated
            return null
        }
    }
}

@Suppress("UnusedReceiverParameter")
context(relationProvider: KaFirSymbolRelationProvider)
private inline fun <reified P : KaSymbol, R : KaSymbol> KaDeclarationSymbol.getExpectsForActualParent(
    actualParent: P?,
    transformer: (P) -> R?,
): List<R> {
    return (actualParent as? KaDeclarationSymbol)?.let { relationProvider.getExpectsForActual(it) }
        .orEmpty()
        .filterIsInstance<P>()
        .mapNotNull(transformer)
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun computeExpectsForLibraryClass(
    actualSymbol: FirClassLikeSymbol<*>,
    actualModule: KaModule,
    expectDeclarationProvider: KotlinDeclarationProvider,
): List<FirClassLikeSymbol<*>> {
    val implementingPlatform = actualModule.targetPlatform

    // Even for a type alias, the 'expect' counterpart will be a class ('expect typealias' is prohibited)
    val declarations = expectDeclarationProvider.getAllClassesByClassId(actualSymbol.classId)

    return buildList {
        for (declaration in declarations) {
            if (!declaration.isExpectDeclaration()) {
                continue
            }

            val declarationPlatforms = resolutionFacade.getModule(declaration).targetPlatform
            if (declarationPlatforms.intersect(implementingPlatform.componentPlatforms).isEmpty()) {
                continue
            }

            val expectSymbol = declaration.resolveToFirSymbolOfType<FirClassLikeSymbol<*>>(resolutionFacade)
            if (expectSymbol != actualSymbol) {
                add(expectSymbol)
            }
        }
    }
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun computeExpectsForLibraryCallable(
    actualSymbol: FirCallableSymbol<*>,
    actualModule: KaModule,
    expectDeclarationProvider: KotlinDeclarationProvider,
): List<FirCallableSymbol<*>> {
    @OptIn(ClassIdBasedLocality::class)
    val callableId = actualSymbol.callableId?.takeUnless { it.isLocal } ?: return emptyList()

    val actualSession = actualSymbol.llFirSession

    val result = mutableListOf<FirCallableSymbol<*>>()

    val expectMatchingContext = FirExpectActualMatchingContextImpl.Factory.create(
        actualSession,
        actualSession.getScopeSession(),
        allowedWritingMemberExpectForActualMapping = false
    )

    fun handle(expectSymbol: FirCallableSymbol<*>, expectClass: FirRegularClassSymbol?, actualClass: FirRegularClassSymbol?) {
        when {
            expectSymbol == actualSymbol -> {
                /** Found the same symbol (possible as [expectDeclarationProvider] contains the [actualSymbol]. */
                return
            }

            !expectSymbol.isExpect -> return
            expectSymbol.isSubstitutionOrIntersectionOverride -> return
            expectSymbol.visibility == Visibilities.Private -> return
        }

        val compatibility = AbstractExpectActualMatcher
            .getCallablesMatchingCompatibility(expectSymbol, actualSymbol, expectClass, actualClass, expectMatchingContext)

        if (compatibility == ExpectActualMatchingCompatibility.MatchedSuccessfully) {
            result.add(expectSymbol)
        }
    }

    if (callableId.classId != null) {
        val actualClass = actualSymbol.getContainingClassSymbol()
        if (actualClass !is FirRegularClassSymbol || actualClass.isExpect) {
            return emptyList()
        }

        val expectClasses = computeExpectsForLibraryClass(actualClass, actualModule, expectDeclarationProvider)

        for (expectClass in expectClasses) {
            if (expectClass !is FirRegularClassSymbol) {
                continue
            }

            fun FirScope.processRelevantDeclarations(processor: (FirCallableSymbol<*>) -> Unit) {
                when (actualSymbol) {
                    is FirFunctionSymbol -> processFunctionsByName(actualSymbol.name, processor)
                    is FirPropertySymbol -> processPropertiesByName(actualSymbol.name, processor)
                }
            }

            when {
                actualSymbol is FirConstructorSymbol -> {
                    val scope = expectClass.declaredMemberScope(actualSession, memberRequiredPhase = null)
                    scope.processDeclaredConstructors { handle(it, expectClass, actualClass) }
                }
                actualSymbol.isStatic -> {
                    val scope = expectClass.staticScope(SessionHolderImpl(actualSession, actualSession.getScopeSession()))
                    scope?.processRelevantDeclarations { handle(it, expectClass, actualClass) }
                }
                else -> {
                    val scope = expectClass.declaredMemberScope(actualSession, memberRequiredPhase = null)
                    scope.processRelevantDeclarations { handle(it, expectClass, actualClass) }
                }
            }
        }
    } else {
        val candidates = when (actualSymbol) {
            is FirFunctionSymbol -> expectDeclarationProvider.getTopLevelFunctions(callableId)
            is FirPropertySymbol -> expectDeclarationProvider.getTopLevelProperties(callableId)
            else -> emptyList()
        }

        for (candidate in candidates) {
            if (!candidate.hasExpectModifier() || candidate.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                // Quick PSI-level check for inapplicable declarations (required 'expect' and 'EXPECTED_PRIVATE_DECLARATION')
                continue
            }

            val expectSymbol = candidate.resolveToFirSymbolOfType<FirCallableSymbol<*>>(resolutionFacade)
            handle(expectSymbol, expectClass = null, actualClass = null)
        }
    }

    return result
}
