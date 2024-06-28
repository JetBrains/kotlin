/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.isPrimitiveType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal fun FirExpression.unwrapToMoreUsefulExpression() = when (this) {
    is FirWhenSubjectExpression -> whenRef.value.subject ?: this
    else -> this
}

internal class TypeInfo(
    val type: ConeKotlinType,
    val notNullType: ConeKotlinType,
    val directType: ConeKotlinType,
    val isEnumClass: Boolean,
    val isPrimitive: Boolean,
    val isBuiltin: Boolean,
    val isValueClass: Boolean,
    val isFinal: Boolean,
    val isClass: Boolean,
    val canHaveSubtypesAccordingToK1: Boolean,
) {
    override fun toString() = "$type"
}

private val FirClassSymbol<*>.isBuiltin get() = isPrimitiveType() || classId == StandardClassIds.String || isEnumClass

internal val TypeInfo.isNullableEnum get() = isEnumClass && type.isNullable

internal val TypeInfo.isIdentityLess get() = isPrimitive || isValueClass

internal val TypeInfo.isNotNullPrimitive get() = isPrimitive && !type.isNullable

private val FirClassSymbol<*>.isFinalClass get() = isClass && isFinal

// NB: This is what RULES1 means then it says "class".
private val FirClassSymbol<*>.isClass get() = !isInterface

internal fun ConeKotlinType.isEnum(session: FirSession) = toRegularClassSymbol(session)?.isEnumClass == true

internal fun ConeKotlinType.isClass(session: FirSession) = toRegularClassSymbol(session) != null

internal fun ConeKotlinType.toTypeInfo(session: FirSession): TypeInfo {
    val bounds = collectUpperBounds().map { it.replaceArgumentsWithStarProjections() }
    val type = bounds.ifNotEmpty { ConeTypeIntersector.intersectTypes(session.typeContext, this) }?.fullyExpandedType(session)
        ?: session.builtinTypes.nullableAnyType.type
    val notNullType = type.withNullability(ConeNullability.NOT_NULL, session.typeContext)
    val boundsSymbols = bounds.mapNotNull { it.toClassSymbol(session) }

    return TypeInfo(
        type, notNullType, directType = this,
        isEnumClass = boundsSymbols.any { it.isEnumClass },
        isPrimitive = bounds.any { it.isPrimitiveOrNullablePrimitive },
        isBuiltin = boundsSymbols.any { it.isBuiltin },
        isValueClass = boundsSymbols.any { it.isInline },
        isFinal = boundsSymbols.any { it.isFinalClass },
        isClass = boundsSymbols.any { it.isClass },
        // In K1's intersector, `canHaveSubtypes()` is called for `nullabilityStripped`.
        withNullability(ConeNullability.NOT_NULL, session.typeContext).canHaveSubtypesAccordingToK1(session),
    )
}

internal fun ConeKotlinType.toKotlinTypeIfPlatform(session: FirSession): ConeClassLikeType? =
    session.platformClassMapper.getCorrespondingKotlinClass(classId)?.constructClassLikeType(typeArguments, isNullable, attributes)

internal fun ConeKotlinType.toPlatformTypeIfKotlin(session: FirSession): ConeClassLikeType? =
    session.platformClassMapper.getCorrespondingPlatformClass(classId)?.constructClassLikeType(typeArguments, isNullable, attributes)

internal class ArgumentInfo(
    val argument: FirExpression,
    val userType: ConeKotlinType,
    val originalType: ConeKotlinType,
    val session: FirSession,
) {
    val smartCastType: ConeKotlinType by lazy { userType.fullyExpandedType(session) }

    val originalTypeInfo get() = originalType.toTypeInfo(session)

    val smartCastTypeInfo get() = smartCastType.toTypeInfo(session)

    override fun toString() = "${argument.source?.text} :: $userType"
}

@Suppress("RecursivePropertyAccessor")
private val FirExpression.mostOriginalTypeIfSmartCast: ConeKotlinType
    get() = when (this) {
        is FirSmartCastExpression -> originalExpression.mostOriginalTypeIfSmartCast
        else -> resolvedType
    }

internal fun FirExpression.toArgumentInfo(context: CheckerContext) =
    ArgumentInfo(
        this,
        userType = resolvedType.finalApproximationOrSelf(context),
        originalType = mostOriginalTypeIfSmartCast.fullyExpandedType(context.session).finalApproximationOrSelf(context),
        context.session,
    )

private fun ConeKotlinType.getCounterpartRelativelyToPlatform(session: FirSession): ConeKotlinType? =
    toKotlinTypeIfPlatform(session) ?: toPlatformTypeIfKotlin(session)

/**
 * This function de-facto replicates a single-side check from [org.jetbrains.kotlin.types.CastDiagnosticsUtil.isRelated].
 */
private fun TypeInfo.isSubtypeOf(other: TypeInfo, context: CheckerContext): Boolean {
    val isDirectSubtype = notNullType.isSubtypeOf(other.notNullType, context.session)
    val counterpart = other.notNullType.getCounterpartRelativelyToPlatform(context.session)
    return isDirectSubtype || counterpart?.let { notNullType.isSubtypeOf(it, context.session) } == true
}

internal fun areUnrelated(a: TypeInfo, b: TypeInfo, context: CheckerContext): Boolean {
    return !a.isSubtypeOf(b, context) && !b.isSubtypeOf(a, context)
}

internal fun areRelated(a: TypeInfo, b: TypeInfo, context: CheckerContext): Boolean = !areUnrelated(a, b, context)

/**
 * See [KT-57779](https://youtrack.jetbrains.com/issue/KT-57779) for more information.
 */
internal fun shouldReportAsPerRules1(l: TypeInfo, r: TypeInfo, context: CheckerContext): Boolean {
    val oneIsFinal = l.isFinal || r.isFinal

    return when {
        oneIsFinal -> areUnrelated(l, r, context)
        else -> false
    }
}
