/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.analysis.FirOverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.dispatchReceiverTypeOrNull
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId

/**
 * Helper that determines if `override` keyword can be omitted for certain overrides. In general it can be omitted if
 *
 * 1. the super member is annotated with `@kotlin.internal.PlatformDependent`, OR
 * 2. the super member is declared in a Java class that has special Kotlin class mapping. For example, overriding members of
 *    `kotlin.Throwable` does not require `override` keyword.
 *
 * Note that, in case of multi-override, if any super member requires `override`, then the `override` keyword cannot be omitted.
 */
object FirJvmOverridesBackwardCompatibilityHelper : FirOverridesBackwardCompatibilityHelper {
    private val javaOrigin = setOf(FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement)
    private val platformDependentAnnotation = ClassId.fromString("kotlin/internal/PlatformDependent")

    override fun overrideCanBeOmitted(
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext
    ): Boolean {
        // Members could share the same common interface up in the hierarchy. Hence we track the visited members to avoid redundant work.
        val visitedSymbols = hashSetOf<FirCallableSymbol<*>>()
        return overriddenMemberSymbols.all { isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(it, visitedSymbols, context) }
    }

    private fun isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(
        symbol: FirCallableSymbol<*>,
        visitedSymbols: MutableSet<FirCallableSymbol<*>>,
        context: CheckerContext
    ): Boolean {
        val fir = symbol.fir as? FirCallableMemberDeclaration<*> ?: return false
        if (fir.isFinal) return false

        if (symbol in visitedSymbols) return true
        visitedSymbols += symbol

        val originalMember = fir.originalOrSelf()
        if (originalMember.annotations.any { it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId == platformDependentAnnotation }) {
            return true
        }

        if (originalMember.origin !in javaOrigin) return false
        val containingClassName = originalMember.containingClass()?.classId?.asSingleFqName()?.toUnsafe() ?: return false
        // If the super class is mapped to a Kotlin built-in class, then we don't require `override` keyword.
        if (JavaToKotlinClassMap.mapKotlinToJava(containingClassName) != null) return true

        val scope =
            symbol.dispatchReceiverTypeOrNull()?.toRegularClass(context.session)?.unsubstitutedScope(context) ?: return false
        val overriddenSymbols = when (originalMember) {
            is FirSimpleFunction -> scope.getDirectOverriddenFunctions(originalMember.symbol)
            is FirProperty -> scope.getDirectOverriddenProperties(originalMember.symbol)
            else -> return false
        }
        if (overriddenSymbols.isEmpty()) return false
        return overriddenSymbols.all { isPlatformSpecificSymbolThatCanBeImplicitlyOverridden(it, visitedSymbols, context) }
    }
}
