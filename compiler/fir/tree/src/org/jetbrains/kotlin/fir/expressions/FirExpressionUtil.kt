/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirBlockImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.TransformData
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

inline val FirAnnotation.unexpandedConeClassLikeType: ConeClassLikeType?
    get() = ((annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)

inline val FirAnnotation.unexpandedClassId: ClassId?
    get() = unexpandedConeClassLikeType?.lookupTag?.classId

fun <T> buildConstOrErrorExpression(source: KtSourceElement?, kind: ConstantValueKind<T>, value: T?, diagnostic: ConeDiagnostic): FirExpression =
    value?.let {
        buildConstExpression(source, kind, it)
    } ?: buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
    }

inline val FirCall.arguments: List<FirExpression> get() = argumentList.arguments

inline val FirCall.argument: FirExpression get() = argumentList.arguments.first()

inline val FirCall.dynamicVararg: FirVarargArgumentsExpression?
    get() = arguments.firstOrNull() as? FirVarargArgumentsExpression

inline val FirCall.dynamicVarargArguments: List<FirExpression>?
    get() = dynamicVararg?.arguments

inline val FirFunctionCall.isCalleeDynamic: Boolean
    get() = calleeReference.toResolvedFunctionSymbol()?.origin == FirDeclarationOrigin.DynamicScope

inline val FirCall.resolvedArgumentMapping: LinkedHashMap<FirExpression, FirValueParameter>?
    get() = when (val argumentList = argumentList) {
        is FirResolvedArgumentList -> argumentList.mapping
        else -> null
    }

fun FirExpression.toResolvedCallableReference(): FirResolvedNamedReference? {
    return toReference()?.resolved
}

fun FirExpression.toReference(): FirReference? {
    return when (this) {
        is FirWrappedArgumentExpression -> expression.toResolvedCallableReference()
        is FirSmartCastExpression -> originalExpression.toReference()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.toReference()
        is FirResolvable -> calleeReference
        else -> null
    }
}

fun FirExpression.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as? FirCallableSymbol<*>?
}

fun buildErrorLoop(source: KtSourceElement?, diagnostic: ConeDiagnostic): FirErrorLoop {
    return buildErrorLoop {
        this.source = source
        this.diagnostic = diagnostic
    }.also {
        it.block.replaceTypeRef(buildErrorTypeRef {
            this.diagnostic = diagnostic
        })
    }
}

fun buildErrorExpression(
    source: KtSourceElement?,
    diagnostic: ConeDiagnostic,
    element: FirElement? = null
): FirErrorExpression {
    return buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
        this.expression = element as? FirExpression
        this.nonExpressionElement = element.takeUnless { it is FirExpression }
    }
}

fun <D> FirBlock.transformStatementsIndexed(transformer: FirTransformer<D>, dataProducer: (Int) -> TransformData<D>): FirBlock {
    when (this) {
        is FirBlockImpl -> statements.transformInplace(transformer, dataProducer)
        is FirSingleExpressionBlock -> {
            (dataProducer(0) as? TransformData.Data<D>)?.value?.let { transformStatements(transformer, it) }
        }
    }
    return this
}

fun <T : FirStatement> FirBlock.replaceFirstStatement(factory: (T) -> FirStatement): T {
    require(this is FirBlockImpl) {
        "replaceFirstStatement should not be called for ${this::class.simpleName}"
    }
    @Suppress("UNCHECKED_CAST")
    val existing = statements[0] as T
    statements[0] = factory(existing)
    return existing
}

fun FirExpression.unwrapArgument(): FirExpression = (this as? FirWrappedArgumentExpression)?.expression ?: this

fun FirExpression.unwrapAndFlattenArgument(): List<FirExpression> = buildList { unwrapAndFlattenArgumentTo(this) }

private fun FirExpression.unwrapAndFlattenArgumentTo(list: MutableList<FirExpression>) {
    when (val unwrapped = unwrapArgument()) {
        is FirArrayOfCall, is FirFunctionCall -> (unwrapped as FirCall).arguments.forEach { it.unwrapAndFlattenArgumentTo(list) }
        is FirVarargArgumentsExpression -> unwrapped.arguments.forEach { it.unwrapAndFlattenArgumentTo(list) }
        else -> list.add(unwrapped)
    }
}

val FirVariableAssignment.explicitReceiver: FirExpression? get() = unwrapLValue()?.explicitReceiver

val FirVariableAssignment.dispatchReceiver: FirExpression get() = unwrapLValue()?.dispatchReceiver ?: FirNoReceiverExpression

val FirVariableAssignment.extensionReceiver: FirExpression get() = unwrapLValue()?.extensionReceiver ?: FirNoReceiverExpression

val FirVariableAssignment.calleeReference: FirReference? get() = lValue.toReference()

val FirVariableAssignment.contextReceiverArguments: List<FirExpression> get() = unwrapLValue()?.contextReceiverArguments ?: emptyList()

fun FirVariableAssignment.unwrapLValue(): FirQualifiedAccessExpression? {
    val lValue = lValue
    return lValue as? FirQualifiedAccessExpression
        ?: (lValue as? FirDesugaredAssignmentValueReferenceExpression)?.expressionRef?.value as? FirQualifiedAccessExpression
}

val FirElement.calleeReference: FirReference?
    get() = (this as? FirResolvable)?.calleeReference ?: (this as? FirVariableAssignment)?.calleeReference

fun FirExpression.unwrapSmartcastExpression(): FirExpression =
    when (this) {
        is FirSmartCastExpression -> originalExpression
        else -> this
    }

/**
 * A callable reference is bound iff
 * - one of [dispatchReceiver] or [extensionReceiver] is **not** [FirNoReceiverExpression] and
 * - it's not referring to a static member.
 */
val FirCallableReferenceAccess.isBound: Boolean
    get() = (dispatchReceiver != FirNoReceiverExpression || extensionReceiver != FirNoReceiverExpression) &&
            calleeReference.toResolvedCallableSymbol()?.isStatic != true

val FirQualifiedAccessExpression.allReceiverExpressions: List<FirExpression>
    get() = buildList {
        addIfNotNull(dispatchReceiver)
        addIfNotNull(extensionReceiver)
        addAll(contextReceiverArguments)
    }
