/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun FirExpectActualMatchingContext.areFirAnnotationsEqual(
    annotation1: FirAnnotation,
    annotation2: FirAnnotation,
    collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    actualSession: FirSession,
): Boolean {
    fun FirAnnotation.hasResolvedArguments(): Boolean {
        return resolved || (this is FirAnnotationCall && arguments.isEmpty())
    }

    check(annotation1.hasResolvedArguments() && annotation2.hasResolvedArguments()) {
        "By this time compared annotations are expected to have resolved arguments"
    }
    if (!areCompatibleExpectActualTypes(
            annotation1.resolvedType, annotation2.resolvedType, parameterOfAnnotationComparisonMode = false
        )
    ) {
        return false
    }
    val args1 = FirExpressionEvaluator.evaluateAnnotationArguments(annotation1, actualSession) ?: return false
    val args2 = FirExpressionEvaluator.evaluateAnnotationArguments(annotation2, actualSession) ?: return false
    if (args1.size != args2.size) {
        return false
    }
    return args1.all { (key, value1) ->
        val value2 = args2[key]
        value1 is FirEvaluatorResult.Evaluated && value2 is FirEvaluatorResult.Evaluated &&
                areAnnotationArgumentsEqual(value1.result, value2.result, collectionArgumentsCompatibilityCheckStrategy)
    }
}

private fun FirExpectActualMatchingContext.mappingsAreEqual(
    argumentMapping1: FirAnnotationArgumentMapping,
    argumentMapping2: FirAnnotationArgumentMapping,
    collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy
): Boolean {
    return argumentMapping1.mapping.keys.all { name ->
        areAnnotationArgumentsEqual(
            argumentMapping1.mapping[name],
            argumentMapping2.mapping[name],
            collectionArgumentsCompatibilityCheckStrategy,
        )
    }
}

private fun FirExpectActualMatchingContext.areAnnotationArgumentsEqual(
    expression1: FirElement?,
    expression2: FirElement?,
    collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy
): Boolean {
    fun List<FirExpression>.unwrapSpreadOperator(): List<FirExpression> {
        return this.flatMap {
            if (it is FirSpreadArgumentExpression && it.expression is FirArrayLiteral) {
                (it.expression as FirArrayLiteral).argumentList.arguments
            } else {
                listOf(it)
            }
        }
    }

    fun argumentsOfArrayAreEqual(arguments1: List<FirExpression>, arguments2: List<FirExpression>): Boolean {
        return collectionArgumentsCompatibilityCheckStrategy.areCompatible(
            arguments1.unwrapSpreadOperator(), arguments2.unwrapSpreadOperator()
        ) { f, s ->
            areAnnotationArgumentsEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
        }
    }

    fun FirVarargArgumentsExpression.isEqualTo(other: FirElement): Boolean {
        return when (other) {
            is FirVarargArgumentsExpression -> argumentsOfArrayAreEqual(this.arguments, other.arguments)
            is FirArrayLiteral -> argumentsOfArrayAreEqual(this.arguments, other.arguments)
            else -> false
        }
    }

    fun FirArrayLiteral.isEqualTo(other: FirElement): Boolean {
        return when (other) {
            is FirVarargArgumentsExpression -> other.isEqualTo(this)
            is FirArrayLiteral -> argumentsOfArrayAreEqual(this.arguments, other.arguments)
            else -> false
        }
    }

    fun FirQualifiedAccessExpression.isEqualTo(other: FirElement): Boolean {
        return when (other) {
            is FirQualifiedAccessExpression -> {
                val symbol1 = this.toResolvedCallableSymbol()
                val symbol2 = other.toResolvedCallableSymbol()
                when {
                    symbol1 is FirEnumEntrySymbol && symbol2 is FirEnumEntrySymbol -> {
                        areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) && symbol1.name == symbol2.name
                    }
                    symbol1 is FirConstructorSymbol && symbol2 is FirConstructorSymbol -> {
                        val constructorCall1 = this as FirFunctionCall
                        val constructorCall2 = other as FirFunctionCall

                        val annotationMapping1 = (constructorCall1.argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping()
                        val annotationMapping2 = (constructorCall2.argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping()

                        areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) &&
                                mappingsAreEqual(annotationMapping1, annotationMapping2, collectionArgumentsCompatibilityCheckStrategy)
                    }
                    else -> false
                }
            }
            is FirEnumEntryDeserializedAccessExpression -> {
                when (val symbol1 = this.toResolvedCallableSymbol()) {
                    !is FirEnumEntrySymbol -> false
                    else -> areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) && symbol1.name == other.enumEntryName
                }
            }
            is FirAnnotation -> {
                when (this.toResolvedCallableSymbol()) {
                    !is FirConstructorSymbol -> return false
                    else -> {
                        val constructorCall1 = this as FirFunctionCall
                        val annotationMapping1 = (constructorCall1.argumentList as FirResolvedArgumentList).toAnnotationArgumentMapping()
                        areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) &&
                                mappingsAreEqual(annotationMapping1, other.argumentMapping, collectionArgumentsCompatibilityCheckStrategy)
                    }
                }
            }
            else -> false
        }
    }

    fun FirEnumEntryDeserializedAccessExpression.isEqualTo(other: FirElement): Boolean {
        return when (other) {
            is FirQualifiedAccessExpression -> other.isEqualTo(this)
            is FirEnumEntryDeserializedAccessExpression -> {
                areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) && this.enumEntryName == other.enumEntryName
            }
            else -> false
        }
    }

    fun FirAnnotation.isEqualTo(other: FirElement): Boolean {
        return when (other) {
            is FirQualifiedAccessExpression -> other.isEqualTo(this)
            is FirAnnotation -> {
                areCompatibleExpectActualTypes(this.resolvedType, other.resolvedType) &&
                        mappingsAreEqual(this.argumentMapping, other.argumentMapping, collectionArgumentsCompatibilityCheckStrategy)
            }
            else -> false
        }
    }

    // All these mutual checks are required because FIR from sources is different from FIR from dependency.
    // But we still count it as equals.
    // For example, enum entry from dependency will be represented as `FirEnumEntryDeserializedAccessExpression`,
    // but enum entry from sources will be `FirQualifiedAccessExpression`.
    return when {
        expression1 == null || expression2 == null -> (expression1 == null) == (expression2 == null)

        expression1 is FirLiteralExpression && expression2 is FirLiteralExpression -> expression1.value == expression2.value

        expression1 is FirGetClassCall && expression2 is FirGetClassCall -> {
            areCompatibleExpectActualTypes(expression1.resolvedType, expression2.resolvedType)
        }

        expression1 is FirQualifiedAccessExpression -> expression1.isEqualTo(expression2)
        expression2 is FirQualifiedAccessExpression -> expression2.isEqualTo(expression1)

        expression1 is FirAnnotation -> expression1.isEqualTo(expression2)
        expression2 is FirAnnotation -> expression2.isEqualTo(expression1)

        expression1 is FirEnumEntryDeserializedAccessExpression -> expression1.isEqualTo(expression2)
        expression2 is FirEnumEntryDeserializedAccessExpression -> expression2.isEqualTo(expression1)

        expression1 is FirArrayLiteral -> expression1.isEqualTo(expression2)
        expression2 is FirArrayLiteral -> expression2.isEqualTo(expression1)

        expression1 is FirVarargArgumentsExpression -> expression1.isEqualTo(expression2)
        expression2 is FirVarargArgumentsExpression -> expression2.isEqualTo(expression1)

        else -> errorWithAttachment("Not handled expression types") {
            withFirEntry("expression1", expression1)
            withFirEntry("expression2", expression2)
        }
    }
}
