/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenSymbolsSafe
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId

/**
 * Helper that determines if `override` keyword can be omitted for certain overrides.
 * In general, it can be omitted if
 *
 * 1. the super member is annotated with `@kotlin.internal.PlatformDependent`, OR
 * 2. the additionalCheck returns not null result.
 *    This check can be implemented in subclass
 *
 * Note that, in case of multi-override, if any super member requires `override`, then the `override` keyword cannot be omitted.
 */
abstract class FirOverridesBackwardCompatibilityHelper : FirComposableSessionComponent<FirOverridesBackwardCompatibilityHelper> {
    private val platformDependentAnnotation = ClassId.fromString("kotlin/internal/PlatformDependent")

    context(context: CheckerContext)
    open fun overrideCanBeOmitted(
        overriddenMemberSymbols: List<FirCallableSymbol<*>>
    ): Boolean {
        // Members could share the same common interface up in the hierarchy. Hence, we track the visited members to avoid redundant work.
        val visitedSymbols = hashSetOf<FirCallableSymbol<*>>()
        return overriddenMemberSymbols.all { isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(it, visitedSymbols) }
    }

    context(context: CheckerContext)
    private fun isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(
        symbol: FirCallableSymbol<*>,
        visitedSymbols: MutableSet<FirCallableSymbol<*>>
    ): Boolean {
        if (symbol.isFinal) return false

        if (!visitedSymbols.add(symbol)) return true

        val originalMemberSymbol = symbol.originalOrSelf()
        originalMemberSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        if (originalMemberSymbol.hasAnnotation(platformDependentAnnotation, context.session)) {
            return true
        }

        additionalCheck(originalMemberSymbol)?.let { return it }

        if (!originalMemberSymbol.isAbstract) {
            val containingClass = originalMemberSymbol.containingClassLookupTag()?.toRegularClassSymbol()
            if (containingClass?.isInterface == false) {
                return false
            }
        }

        val overriddenSymbols = originalMemberSymbol.directOverriddenSymbolsSafe()
        if (overriddenSymbols.isEmpty()) return false
        return overriddenSymbols.all { isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(it, visitedSymbols) }
    }

    protected open fun additionalCheck(member: FirCallableSymbol<*>): Boolean? = null

    class Composed(
        override val components: List<FirOverridesBackwardCompatibilityHelper>
    ) : FirOverridesBackwardCompatibilityHelper(), FirComposableSessionComponent.Composed<FirOverridesBackwardCompatibilityHelper> {
        context(context: CheckerContext)
        override fun overrideCanBeOmitted(overriddenMemberSymbols: List<FirCallableSymbol<*>>): Boolean {
            return components.all { it.overrideCanBeOmitted(overriddenMemberSymbols) }
        }

        override fun additionalCheck(member: FirCallableSymbol<*>): Boolean? {
            return components.firstNotNullOfOrNull { it.additionalCheck(member) }
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirOverridesBackwardCompatibilityHelper>): Composed {
        return Composed(components)
    }
}

val FirSession.overridesBackwardCompatibilityHelper: FirOverridesBackwardCompatibilityHelper
        by FirSession.sessionComponentAccessorWithDefault(FirDefaultOverridesBackwardCompatibilityHelper)
