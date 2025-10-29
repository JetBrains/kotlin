/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef

context(context: CheckerContext)
internal fun isInsideExpectClass(containingClass: FirClassSymbol<*>): Boolean {
    return isInsideSpecificClass(containingClass) { klass -> klass is FirRegularClassSymbol && klass.isExpect }
}

context(context: CheckerContext)
internal fun isInsideExternalClass(containingClass: FirClassSymbol<*>): Boolean {
    return isInsideSpecificClass(containingClass) { klass -> klass is FirRegularClassSymbol && klass.isExternal }
}

context(context: CheckerContext)
// Note that the class that contains the currently visiting declaration will *not* be in the context's containing declarations *yet*.
private inline fun isInsideSpecificClass(
    containingClass: FirClassSymbol<*>,
    predicate: (FirClassSymbol<*>) -> Boolean
): Boolean {
    return predicate.invoke(containingClass) ||
            context.containingDeclarations.asReversed().any { it is FirRegularClassSymbol && predicate.invoke(it) }
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

context(context: CheckerContext)
internal fun FirMemberDeclaration.isEffectivelyExpect(
    containingClass: FirClassSymbol<*>?,
): Boolean {
    if (this.isExpect) return true

    return containingClass != null && isInsideExpectClass(containingClass)
}

context(context: CheckerContext)
@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.isEffectivelyExpect(
    containingClass: FirClassSymbol<*>?,
): Boolean = fir.isEffectivelyExpect(containingClass)

context(context: CheckerContext)
internal fun FirMemberDeclaration.isEffectivelyExternal(
    containingClass: FirClassSymbol<*>?,
): Boolean {
    if (this.isExternal) return true

    if (this is FirPropertyAccessor) {
        // Check containing property
        val property = context.containingDeclarations.last() as FirPropertySymbol
        return property.isEffectivelyExternal(containingClass)
    }

    if (this is FirProperty) {
        // Property is effectively external if all accessors are external
        if (getter?.isExternal == true && (!isVar || setter?.isExternal == true)) {
            return true
        }
    }

    return containingClass != null && isInsideExternalClass(containingClass)
}

context(context: CheckerContext)
@OptIn(SymbolInternals::class)
internal fun FirCallableSymbol<*>.isEffectivelyExternal(
    containingClass: FirClassSymbol<*>?,
): Boolean = fir.isEffectivelyExternal(containingClass)

internal val FirClass.canHaveOpenMembers: Boolean get() = modality() != Modality.FINAL || classKind == ClassKind.ENUM_CLASS

/**
 * Similar to [FirMemberDeclaration.isLocal], but returns false for callable members of local classes.
 *
 * @return true for local classes, including nested ones, and for local callables declared inside a block
 * (e.g., inside a function / accessor / initializer body), excluding callable members of local classes
 */
val FirDeclaration.isLocalDeclaredInBlock: Boolean
    // contract: returns(true) implies (this is FirMemberDeclaration<*>)
    get() = if (this is FirClassLikeDeclaration) isLocal else when (val symbol = symbol) {
        is FirLocalPropertySymbol -> true
        is FirNamedFunctionSymbol -> symbol.rawStatus.visibility == Visibilities.Local
        // Anonymous functions and lambdas use DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS which has visibility public.
        is FirAnonymousFunctionSymbol -> true
        is FirBackingFieldSymbol -> symbol.propertySymbol is FirLocalPropertySymbol
        else -> false
    }

internal val FirCallableSymbol<*>.isExtensionMember: Boolean
    get() = resolvedReceiverTypeRef != null && dispatchReceiverType != null

fun FirTypeRef.needsMultiFieldValueClassFlattening(session: FirSession): Boolean = coneType.needsMultiFieldValueClassFlattening(session)

fun ConeKotlinType.needsMultiFieldValueClassFlattening(session: FirSession): Boolean = with(session.typeContext) {
    typeConstructor().isMultiFieldValueClass() && !fullyExpandedType(session).isMarkedNullable
}

val FirCallableSymbol<*>.hasExplicitReturnType: Boolean
    get() {
        val returnTypeRef = resolvedReturnTypeRef
        return returnTypeRef.delegatedTypeRef != null || returnTypeRef is FirImplicitUnitTypeRef
    }

fun FirNamedFunctionSymbol.checkValueParameterNamesWith(
    otherFunctionSymbol: FirNamedFunctionSymbol,
    reportAction: (currentParameter: FirValueParameterSymbol, conflictingParameter: FirValueParameterSymbol, parameterIndex: Int) -> Unit,
) {
    checkValueParameterNamesWith(valueParameterSymbols, otherFunctionSymbol.valueParameterSymbols, reportAction)
    checkValueParameterNamesWith(contextParameterSymbols, otherFunctionSymbol.contextParameterSymbols, reportAction)
}

@OptIn(SymbolInternals::class)
private fun checkValueParameterNamesWith(
    symbols: List<FirValueParameterSymbol>,
    otherSymbols: List<FirValueParameterSymbol>,
    reportAction: (FirValueParameterSymbol, FirValueParameterSymbol, Int) -> Unit,
) {
    val valueParameterPairs = symbols.zip(otherSymbols)
    for ((index, valueParameterPair) in valueParameterPairs.withIndex()) {
        val (currentValueParameter, otherValueParameter) = valueParameterPair
        if (currentValueParameter.fir.isLegacyContextReceiver() || otherValueParameter.fir.isLegacyContextReceiver()) continue
        if (currentValueParameter.name != otherValueParameter.name) {
            reportAction(currentValueParameter, otherValueParameter, index)
        }
    }
}
