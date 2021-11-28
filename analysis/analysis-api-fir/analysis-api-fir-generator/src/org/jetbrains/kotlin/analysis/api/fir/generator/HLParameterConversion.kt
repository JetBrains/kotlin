/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

sealed class HLParameterConversion {
    abstract fun convertExpression(expression: String, context: ConversionContext): String
    abstract fun convertType(type: KType): KType
    open val importsToAdd: List<String> get() = emptyList()
}

object HLIdParameterConversion : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext) = expression
    override fun convertType(type: KType): KType = type
}

class HLCollectionParameterConversion(
    private val parameterName: String,
    private val mappingConversion: HLParameterConversion,
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext): String {
        val innerExpression = mappingConversion.convertExpression(parameterName, context.increaseIndent())
        return buildString {
            appendLine("$expression.map { $parameterName ->")
            appendLine(innerExpression.withIndent(context.increaseIndent()))
            append("}".withIndent(context))
        }
    }

    override fun convertType(type: KType): KType =
        List::class.createType(
            arguments = listOf(
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = type.arguments.single().type?.let(mappingConversion::convertType)
                )
            )
        )

    override val importsToAdd get() = mappingConversion.importsToAdd
}

class HLMapParameterConversion(
    private val keyName: String,
    private val valueName: String,
    private val mappingConversionForKeys: HLParameterConversion,
    private val mappingConversionForValues: HLParameterConversion,
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext): String {
        val keyTransformation = mappingConversionForKeys.convertExpression(keyName, context.increaseIndent())
        val valueTransformation = mappingConversionForValues.convertExpression(valueName, context.increaseIndent())
        return buildString {
            appendLine("$expression.mapKeys { ($keyName, _) ->")
            appendLine(keyTransformation.withIndent(context.increaseIndent()))
            appendLine("}.mapValues { (_, $valueName) -> ".withIndent(context))
            appendLine(valueTransformation.withIndent(context.increaseIndent()))
            append("}".withIndent(context))
        }
    }

    override fun convertType(type: KType): KType {
        val keyArgument = type.arguments[0]
        val valueArgument = type.arguments[1]
        return Map::class.createType(
            arguments = listOf(
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = keyArgument.type?.let(mappingConversionForKeys::convertType)
                ),
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = valueArgument.type?.let(mappingConversionForValues::convertType)
                )
            )
        )
    }

    override val importsToAdd: List<String>
        get() = (mappingConversionForKeys.importsToAdd + mappingConversionForValues.importsToAdd).distinct()
}

class HLPairParameterConversion(
    private val mappingConversionFirst: HLParameterConversion,
    private val mappingConversionSecond: HLParameterConversion,
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext): String {
        if (mappingConversionFirst.isTrivial && mappingConversionSecond.isTrivial) {
            return expression
        }
        val first = mappingConversionFirst.convertExpression("$expression.first", context)
        val second = mappingConversionSecond.convertExpression("$expression.second", context)
        return "$first to $second"
    }

    override fun convertType(type: KType): KType {
        val first = type.arguments.getOrNull(0)?.type ?: return type
        val second = type.arguments.getOrNull(1)?.type ?: return type
        return Pair::class.createType(
            arguments = listOf(
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = mappingConversionFirst.convertType(first)
                ),
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = mappingConversionSecond.convertType(second)
                )
            )
        )
    }

    override val importsToAdd
        get() = mappingConversionFirst.importsToAdd + mappingConversionSecond.importsToAdd
}

class HLFunctionCallConversion(
    private val callTemplate: String,
    private val callType: KType,
    override val importsToAdd: List<String> = emptyList()
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext) =
        callTemplate.replace("{0}", expression)

    override fun convertType(type: KType): KType = callType
}

data class ConversionContext(val currentIndent: Int, val indentUnitValue: Int) {
    fun increaseIndent() = copy(currentIndent = currentIndent + 1)
}

private fun String.withIndent(context: ConversionContext): String {
    val newIndent = " ".repeat(context.currentIndent * context.indentUnitValue)
    return replaceIndent(newIndent)
}

val HLParameterConversion.isTrivial: Boolean
    get() = this is HLIdParameterConversion
