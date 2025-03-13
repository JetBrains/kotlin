/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef

internal fun isInsideExpectClass(containingClass: FirClass, context: CheckerContext): Boolean {
    return isInsideSpecificClass(containingClass, context) { klass -> klass is FirRegularClass && klass.isExpect }
}

internal fun isInsideExternalClass(containingClass: FirClass, context: CheckerContext): Boolean {
    return isInsideSpecificClass(containingClass, context) { klass -> klass is FirRegularClass && klass.isExternal }
}

// Note that the class that contains the currently visiting declaration will *not* be in the context's containing declarations *yet*.
private inline fun isInsideSpecificClass(
    containingClass: FirClass,
    context: CheckerContext,
    predicate: (FirClass) -> Boolean
): Boolean {
    return predicate.invoke(containingClass) ||
            context.containingDeclarations.asReversed().any { it is FirRegularClass && predicate.invoke(it) }
}

/**
 * The containing symbol is resolved using the declaration-site session.
 */
internal fun FirMemberDeclaration.isEffectivelyFinal(): Boolean =
    this.symbol.isEffectivelyFinal()

/**
 * The containing symbol is resolved using the declaration-site session.
 */
internal fun FirBasedSymbol<*>.isEffectivelyFinal(): Boolean {
    if (this.isFinal()) return true

    val containingClass = this.getContainingClassSymbol() as? FirClassSymbol<*> ?: return true

    if (containingClass.isEnumClass) {
        // Enum class has enum entries and hence is not considered final
        return false
    }
    return containingClass.isFinal
}

private fun FirBasedSymbol<*>.isFinal(): Boolean {
    when (this) {
        is FirCallableSymbol<*> -> if (this.isFinal) return true
        is FirClassLikeSymbol<*> -> if (this.isFinal) return true
        else -> return true
    }

    return false
}

internal fun FirMemberDeclaration.isEffectivelyExpect(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean {
    if (this.isExpect) return true

    return containingClass != null && isInsideExpectClass(containingClass, context)
}

@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.isEffectivelyExpect(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean = fir.isEffectivelyExpect(containingClass, context)

internal fun FirMemberDeclaration.isEffectivelyExternal(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean {
    if (this.isExternal) return true

    if (this is FirPropertyAccessor) {
        // Check containing property
        val property = context.containingDeclarations.last() as FirProperty
        return property.isEffectivelyExternal(containingClass, context)
    }

    if (this is FirProperty) {
        // Property is effectively external if all accessors are external
        if (getter?.isExternal == true && (!isVar || setter?.isExternal == true)) {
            return true
        }
    }

    return containingClass != null && isInsideExternalClass(containingClass, context)
}

@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.isEffectivelyExternal(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean = fir.isEffectivelyExternal(containingClass, context)

internal val FirClass.canHaveOpenMembers: Boolean get() = modality() != Modality.FINAL || classKind == ClassKind.ENUM_CLASS

// contract: returns(true) implies (this is FirMemberDeclaration<*>)
val FirDeclaration.isLocalMember: Boolean
    get() = symbol.isLocalMember

internal val FirBasedSymbol<*>.isLocalMember: Boolean
    get() = when (this) {
        is FirPropertySymbol -> this.isLocal
        is FirRegularClassSymbol -> this.isLocal
        is FirNamedFunctionSymbol -> this.isLocal
        // Anonymous functions and lambdas use DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS which has visibility public.
        is FirAnonymousFunctionSymbol -> true
        else -> false
    }

internal val FirCallableSymbol<*>.isExtensionMember: Boolean
    get() = resolvedReceiverTypeRef != null && dispatchReceiverType != null

fun FirTypeRef.needsMultiFieldValueClassFlattening(session: FirSession): Boolean = coneType.needsMultiFieldValueClassFlattening(session)

fun ConeKotlinType.needsMultiFieldValueClassFlattening(session: FirSession) = with(session.typeContext) {
    typeConstructor().isMultiFieldValueClass() && !fullyExpandedType(session).isMarkedNullable
}

val FirCallableSymbol<*>.hasExplicitReturnType: Boolean
    get() {
        val returnTypeRef = resolvedReturnTypeRef
        return returnTypeRef.delegatedTypeRef != null || returnTypeRef is FirImplicitUnitTypeRef
    }

fun FirNamedFunctionSymbol.checkValueParameterNamesWith(
    otherFunctionSymbol: FirNamedFunctionSymbol,
    reportAction: (currentParameter: FirValueParameterSymbol, conflictingParameter: FirValueParameterSymbol, parameterIndex: Int) -> Unit
) {
    // We don't handle context parameters here because we specifically allow renaming them in overrides (KT-75815).
    val valueParameterPairs = valueParameterSymbols.zip(otherFunctionSymbol.valueParameterSymbols)
    for ((index, valueParameterPair) in valueParameterPairs.withIndex()) {
        val (currentValueParameter, otherValueParameter) = valueParameterPair
        if (currentValueParameter.name != otherValueParameter.name) {
            reportAction(currentValueParameter, otherValueParameter, index)
        }
    }
}
