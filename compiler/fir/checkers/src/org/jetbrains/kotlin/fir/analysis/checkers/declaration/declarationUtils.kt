/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.util.OperatorNameConventions

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

internal fun FirMemberDeclaration.isEffectivelyFinal(context: CheckerContext): Boolean {
    if (this.isFinal) return true
    val containingClass = context.containingDeclarations.lastOrNull() as? FirRegularClass ?: return true
    if (containingClass.isEnumClass) {
        // Enum class has enum entries and hence is not considered final.
        return false
    }
    return containingClass.isFinal
}

internal fun FirMemberDeclaration.isEffectivelyExpect(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean {
    if (this.isExpect) return true

    return containingClass != null && isInsideExpectClass(containingClass, context)
}

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

internal val FirClass.canHaveOpenMembers: Boolean get() = modality() != Modality.FINAL || classKind == ClassKind.ENUM_CLASS

// contract: returns(true) implies (this is FirMemberDeclaration<*>)
val FirDeclaration.isLocalMember: Boolean
    get() = symbol.isLocalMember

internal val FirBasedSymbol<*>.isLocalMember: Boolean
    get() = when (this) {
        is FirPropertySymbol -> this.isLocal
        is FirRegularClassSymbol -> this.isLocal
        is FirNamedFunctionSymbol -> this.isLocal
        else -> false
    }

internal val FirCallableDeclaration.isExtensionMember: Boolean
    get() = symbol.isExtensionMember

internal val FirCallableSymbol<*>.isExtensionMember: Boolean
    get() = resolvedReceiverTypeRef != null && dispatchReceiverType != null

fun FirClassSymbol<*>.primaryConstructorSymbol(): FirConstructorSymbol? {
    for (declarationSymbol in this.declarationSymbols) {
        if (declarationSymbol is FirConstructorSymbol && declarationSymbol.isPrimary) {
            return declarationSymbol
        }
    }
    return null
}

fun FirSimpleFunction.isTypedEqualsInValueClass(session: FirSession): Boolean =
    containingClassLookupTag()?.toFirRegularClassSymbol(session)?.run {
        val valueClassStarProjection = this@run.defaultType().replaceArgumentsWithStarProjections()
        with(this@isTypedEqualsInValueClass) {
            contextReceivers.isEmpty() && receiverParameter == null
                    && name == OperatorNameConventions.EQUALS
                    && this@run.isInline && valueParameters.size == 1
                    && (returnTypeRef.isBoolean || returnTypeRef.isNothing)
                    && valueParameters[0].returnTypeRef.coneType.let { it is ConeClassLikeType && it.replaceArgumentsWithStarProjections() == valueClassStarProjection }
        }
    } ?: false

fun FirTypeRef.needsMultiFieldValueClassFlattening(session: FirSession) = with(session.typeContext) {
    coneType.typeConstructor().isMultiFieldValueClass() && !coneType.isNullable
}

val FirCallableSymbol<*>.hasExplicitReturnType: Boolean
    get() {
        val returnTypeRef = resolvedReturnTypeRef
        return returnTypeRef.delegatedTypeRef != null || returnTypeRef is FirImplicitUnitTypeRef
    }
