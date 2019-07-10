/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsString
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.isExpression
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsStringUnescapedValue
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.resolve.constants.evaluate.*

class ExpressionsConverter(
    private val session: FirSession,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>
) : BaseConverter(session, tree) {

    /*****    EXPRESSIONS    *****/
    fun visitExpression(expression: LighterASTNode): FirExpression {
        if (!stubMode) {
            when (expression.tokenType) {
                STRING_TEMPLATE -> return convertStringTemplate(expression)
                is KtConstantExpressionElementType -> return convertConstantExpression(expression)
            }
            //TODO("not implemented")
        }

        return FirExpressionStub(session, null)
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.ConversionUtilsKt.toInterpolatingCall
     */
    private fun convertStringTemplate(stringTemplate: LighterASTNode): FirExpression {
        val sb = StringBuilder()
        var hasExpressions = false
        var result: FirExpression? = null
        var callCreated = false
        stringTemplate.forEachChildren(OPEN_QUOTE, CLOSING_QUOTE) {
            val nextArgument = when (it.tokenType) {
                LITERAL_STRING_TEMPLATE_ENTRY -> {
                    sb.append(it.getAsString())
                    FirConstExpressionImpl(session, null, IrConstKind.String, it.getAsString())
                }
                ESCAPE_STRING_TEMPLATE_ENTRY -> {
                    sb.append(it.getAsStringUnescapedValue())
                    FirConstExpressionImpl(session, null, IrConstKind.String, it.getAsStringUnescapedValue())
                }
                SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                    hasExpressions = true
                    convertShortOrLongStringTemplate(it)
                }
                else -> {
                    hasExpressions = true
                    FirErrorExpressionImpl(session, null, "Incorrect template entry: ${it.getAsString()}")
                }
            }
            result = when {
                result == null -> nextArgument
                callCreated && result is FirStringConcatenationCallImpl -> (result as FirStringConcatenationCallImpl).apply {
                    //TODO smart cast to FirStringConcatenationCallImpl isn't working
                    arguments += nextArgument
                }
                else -> {
                    callCreated = true
                    FirStringConcatenationCallImpl(session, null).apply {
                        arguments += result!!
                        arguments += nextArgument
                    }
                }
            }
        }
        return if (hasExpressions) result!! else FirConstExpressionImpl(session, null, IrConstKind.String, sb.toString())
    }

    private fun convertShortOrLongStringTemplate(shortOrLongString: LighterASTNode): FirExpression {
        lateinit var firExpression: FirExpression
        shortOrLongString.forEachChildren {
            firExpression = visitExpression(it)
        }
        return firExpression
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.ConversionUtilsKt.generateConstantExpressionByLiteral
     */
    private fun convertConstantExpression(constantExpression: LighterASTNode): FirExpression {
        val type = constantExpression.tokenType
        val text: String = constantExpression.getAsString()
        val convertedText: Any? = when (type) {
            INTEGER_CONSTANT, FLOAT_CONSTANT -> parseNumericLiteral(text, type)
            BOOLEAN_CONSTANT -> parseBoolean(text)
            else -> null
        }

        return when (type) {
            INTEGER_CONSTANT ->
                if (convertedText is Long &&
                    (hasLongSuffix(text) || hasUnsignedLongSuffix(text) || hasUnsignedSuffix(text) ||
                            convertedText > Int.MAX_VALUE || convertedText < Int.MIN_VALUE)
                ) {
                    FirConstExpressionImpl(
                        session, null, IrConstKind.Long, convertedText, "Incorrect long: $text"
                    )
                } else if (convertedText is Number) {
                    // TODO: support byte / short
                    FirConstExpressionImpl(session, null, IrConstKind.Int, convertedText.toInt(), "Incorrect int: $text")
                } else {
                    FirErrorExpressionImpl(session, null, reason = "Incorrect constant expression: $text")
                }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    FirConstExpressionImpl(
                        session, null, IrConstKind.Float, convertedText, "Incorrect float: $text"
                    )
                } else {
                    FirConstExpressionImpl(
                        session, null, IrConstKind.Double, convertedText as Double, "Incorrect double: $text"
                    )
                }
            CHARACTER_CONSTANT ->
                FirConstExpressionImpl(
                    session, null, IrConstKind.Char, text.parseCharacter(), "Incorrect character: $text"
                )
            BOOLEAN_CONSTANT ->
                FirConstExpressionImpl(session, null, IrConstKind.Boolean, convertedText as Boolean)
            NULL ->
                FirConstExpressionImpl(session, null, IrConstKind.Null, null)
            else ->
                throw AssertionError("Unknown literal type: $type, $text")
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseValueArgumentList
     */
    fun convertValueArguments(valueArguments: LighterASTNode): List<FirExpression> {
        return valueArguments.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                VALUE_ARGUMENT -> container += convertValueArgument(node)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseValueArgument
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.toFirExpression(org.jetbrains.kotlin.psi.ValueArgument)
     */
    private fun convertValueArgument(valueArgument: LighterASTNode): FirExpression {
        var identifier: String? = null
        var isSpread = false
        lateinit var firExpression: FirExpression
        valueArgument.forEachChildren {
            when (it.tokenType) {
                VALUE_ARGUMENT_NAME -> identifier = it.getAsString()
                MUL -> isSpread = true
                STRING_TEMPLATE -> firExpression = convertStringTemplate(it)
                is KtConstantExpressionElementType -> firExpression = convertConstantExpression(it)
                else -> if (it.isExpression()) firExpression = visitExpression(it)
            }
        }
        return when {
            identifier != null -> FirNamedArgumentExpressionImpl(session, null, identifier.nameAsSafeName(), isSpread, firExpression)
            isSpread -> FirSpreadArgumentExpressionImpl(session, null, firExpression)
            else -> firExpression
        }
    }
}