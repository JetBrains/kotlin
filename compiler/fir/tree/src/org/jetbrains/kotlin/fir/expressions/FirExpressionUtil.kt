/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirBlockImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirPartiallyResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.TransformData
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.ConstantValueKind

inline val FirAnnotationCall.coneClassLikeType: ConeClassLikeType?
    get() = ((annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)

inline val FirAnnotationCall.classId: ClassId?
    get() = coneClassLikeType?.lookupTag?.classId

fun <T> buildConstOrErrorExpression(source: FirSourceElement?, kind: ConstantValueKind<T>, value: T?, diagnostic: ConeDiagnostic): FirExpression =
    value?.let {
        buildConstExpression(source, kind, it)
    } ?: buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
    }

inline val FirCall.arguments: List<FirExpression> get() = argumentList.arguments

inline val FirCall.argument: FirExpression get() = argumentList.arguments.first()

inline val FirCall.resolvedArgumentMapping: Map<FirExpression, FirValueParameter>?
    get() = when (val argumentList = argumentList) {
        is FirResolvedArgumentList -> argumentList.mapping
        else -> null
    }

inline val FirCall.argumentMapping: Map<FirExpression, FirValueParameter>?
    get() = when (val argumentList = argumentList) {
        is FirResolvedArgumentList -> argumentList.mapping
        is FirPartiallyResolvedArgumentList -> argumentList.mapping
        else -> null
    }

fun FirExpression.toResolvedCallableReference(): FirResolvedNamedReference? {
    if (this is FirWrappedArgumentExpression) return expression.toResolvedCallableReference()
    return (this as? FirResolvable)?.calleeReference as? FirResolvedNamedReference
}

fun FirExpression.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as FirCallableSymbol<*>?
}

fun buildErrorLoop(source: FirSourceElement?, diagnostic: ConeDiagnostic): FirErrorLoop {
    return buildErrorLoop {
        this.source = source
        this.diagnostic = diagnostic
    }
}

fun buildErrorExpression(source: FirSourceElement?, diagnostic: ConeDiagnostic): FirErrorExpression {
    return buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
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

fun <D> FirBlock.transformAllStatementsExceptLast(transformer: FirTransformer<D>, data: D): FirBlock {
    val threshold = statements.size - 1
    return transformStatementsIndexed(transformer) { index ->
        if (index < threshold) {
            TransformData.Data(data)
        } else {
            TransformData.Nothing
        }
    }
}

fun FirBlock.replaceFirstStatement(statement: FirStatement): FirStatement {
    require(this is FirBlockImpl) {
        "replaceFirstStatement should not be called for ${this::class.simpleName}"
    }
    val existed = statements[0]
    statements[0] = statement
    return existed
}

fun FirExpression.unwrapArgument(): FirExpression = (this as? FirWrappedArgumentExpression)?.expression ?: this