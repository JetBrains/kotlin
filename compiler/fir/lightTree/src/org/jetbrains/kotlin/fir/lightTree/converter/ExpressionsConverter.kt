/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsString
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.isExpression
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsStringUnescapedValue
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.extractArgumentsFrom
import org.jetbrains.kotlin.fir.lightTree.converter.FunctionUtil.removeLast
import org.jetbrains.kotlin.fir.lightTree.converter.utils.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class ExpressionsConverter(
    val session: FirSession,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val declarationsConverter: DeclarationsConverter
) : BaseConverter(session, tree) {

    inline fun <reified R : FirElement> getAsFirExpression(expression: LighterASTNode): R {
        return convertExpression(expression) as R
    }

    /*****    EXPRESSIONS    *****/
    fun convertExpression(expression: LighterASTNode): FirElement {
        if (!stubMode) {
            return when (expression.tokenType) {
                BINARY_EXPRESSION -> convertBinaryExpression(expression)
                BINARY_WITH_TYPE -> convertBinaryWithType(expression)
                IS_EXPRESSION -> convertIsExpression(expression)
                PREFIX_EXPRESSION, POSTFIX_EXPRESSION -> convertUnaryExpression(expression)
                ANNOTATED_EXPRESSION -> convertAnnotatedExpression(expression)
                in qualifiedAccessTokens -> convertQualifiedExpression(expression)
                CALL_EXPRESSION -> convertCallExpression(expression)
                ARRAY_ACCESS_EXPRESSION -> convertArrayAccessExpression(expression)
                STRING_TEMPLATE -> convertStringTemplate(expression)
                is KtConstantExpressionElementType -> convertConstantExpression(expression)
                REFERENCE_EXPRESSION -> convertSimpleNameExpression(expression)
                PARENTHESIZED, PROPERTY_DELEGATE, INDICES -> convertExpression(expression.getExpressionInParentheses())
                THIS_EXPRESSION -> convertThisExpression(expression)
                SUPER_EXPRESSION -> convertSuperExpression(expression)
                else -> FirExpressionStub(session, null)
            }
            //TODO("not fully implemented")
        }

        return FirExpressionStub(session, null)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseBinaryExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitBinaryExpression
     */
    private fun convertBinaryExpression(binaryExpression: LighterASTNode): FirStatement {
        var isLeftArgument = true
        lateinit var operationTokenName: String
        lateinit var leftArgNode: LighterASTNode
        lateinit var rightArgAsFir: FirExpression
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    isLeftArgument = false
                    operationTokenName = it.getAsString()
                }
                else -> if (it.isExpression()) {
                    if (isLeftArgument) {
                        leftArgNode = it
                    } else {
                        rightArgAsFir = getAsFirExpression(it)
                    }
                }
            }
        }

        val operationToken = operationTokenName.getOperationSymbol()
        when (operationToken) {
            ELVIS ->
                return getAsFirExpression<FirExpression>(leftArgNode).generateNotNullOrOther(
                    session, rightArgAsFir, "elvis", null
                )
            ANDAND, OROR ->
                return getAsFirExpression<FirExpression>(leftArgNode).generateLazyLogicalOperation(
                    session, rightArgAsFir, operationToken == ANDAND, null
                )
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgAsFir.generateContainsOperation(
                    session, getAsFirExpression(leftArgNode), operationToken == NOT_IN, null, null
                )
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(
                    this@ExpressionsConverter.session, null,
                    conventionCallName ?: operationTokenName.nameAsSafeName()
                )
                explicitReceiver = getAsFirExpression(leftArgNode)
                arguments += rightArgAsFir
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            if (firOperation in FirOperation.ASSIGNMENTS) {
                return convertAssignment(leftArgNode, rightArgAsFir, firOperation)
            } else {
                FirOperatorCallImpl(session, null, firOperation).apply {
                    arguments += getAsFirExpression<FirExpression>(leftArgNode)
                    arguments += rightArgAsFir
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.Precedence.parseRightHandSide
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitBinaryWithTypeRHSExpression
     */
    private fun convertBinaryWithType(binaryExpression: LighterASTNode): FirTypeOperatorCall {
        lateinit var operationTokenName: String
        lateinit var leftArgAsFir: FirExpression
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.getAsString()
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it)
            }
        }

        val operation = operationTokenName.getOperationSymbol().toFirOperation()
        return FirTypeOperatorCallImpl(session, null, operation, firType).apply {
            arguments += leftArgAsFir
        }
    }

    /**
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitIsExpression
     */
    private fun convertIsExpression(isExpression: LighterASTNode): FirTypeOperatorCall {
        lateinit var operationTokenName: String
        lateinit var leftArgAsFir: FirExpression
        lateinit var firType: FirTypeRef
        isExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.getAsString()
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it)
            }
        }

        val operation = if (operationTokenName == "is") FirOperation.IS else FirOperation.NOT_IS
        return FirTypeOperatorCallImpl(session, null, operation, firType).apply {
            arguments += leftArgAsFir
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitUnaryExpression
     */
    private fun convertUnaryExpression(unaryExpression: LighterASTNode): FirExpression {
        lateinit var operationTokenName: String
        lateinit var argument: LighterASTNode
        unaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.getAsString()
                else -> if (it.isExpression()) argument = it
            }
        }

        val operationToken = operationTokenName.getOperationSymbol()
        if (operationToken == EXCLEXCL) {
            return bangBangToWhen(session, getAsFirExpression(argument))
        }

        val conventionCallName = operationToken.toUnaryName()
        return if (conventionCallName != null) {
            if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                return generateIncrementOrDecrementBlock(
                    argument,
                    callName = conventionCallName,
                    prefix = unaryExpression.tokenType == PREFIX_EXPRESSION
                )
            }
            FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, conventionCallName)
                explicitReceiver = getAsFirExpression(argument)
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            FirOperatorCallImpl(session, null, firOperation).apply {
                arguments += getAsFirExpression<FirExpression>(argument)
            }
        }
    }

    private fun convertAnnotatedExpression(annotatedExpression: LighterASTNode): FirElement {
        var firExpression: FirElement? = null
        val firAnnotationList = mutableListOf<FirAnnotationCall>()
        annotatedExpression.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> firAnnotationList += declarationsConverter.convertAnnotation(it)
                ANNOTATION_ENTRY -> firAnnotationList += declarationsConverter.convertAnnotationEntry(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        return (firExpression as? FirAbstractAnnotatedElement)?.apply {
            annotations += firAnnotationList
        } ?: FirErrorExpressionImpl(session, null, "Strange annotated expression: ${firExpression?.render()}")
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitQualifiedExpression
     */
    private fun convertQualifiedExpression(dotQualifiedExpression: LighterASTNode): FirExpression {
        var isSelector = false
        var isSafe = false
        var firSelector: FirExpression? = null //after dot
        lateinit var firReceiver: FirExpression //before dot
        dotQualifiedExpression.forEachChildren {
            when (it.tokenType) {
                DOT -> isSelector = true
                SAFE_ACCESS -> {
                    isSafe = true
                    isSelector = true
                }
                else -> if (isSelector) firSelector = getAsFirExpression(it) else firReceiver = getAsFirExpression(it)
            }
        }

        if (firSelector == null) {
            return FirErrorExpressionImpl(session, null, "Qualified expression without selector")
        }

        //TODO use contracts?
        if (firSelector is FirModifiableQualifiedAccess<*>) {
            (firSelector as FirModifiableQualifiedAccess<*>).safe = isSafe
            (firSelector as FirModifiableQualifiedAccess<*>).explicitReceiver = firReceiver
        }
        return firSelector!!
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseCallSuffix
     */
    private fun convertCallExpression(callSuffix: LighterASTNode): FirExpression {
        var name: String? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        val firValueArguments = mutableListOf<FirExpression>()
        var additionalArgument: FirExpression? = null
        callSuffix.forEachChildren {
            when (it.tokenType) {
                REFERENCE_EXPRESSION -> name = it.getAsString()
                TYPE_ARGUMENT_LIST -> firTypeArguments += declarationsConverter.convertTypeArguments(it)
                VALUE_ARGUMENT_LIST -> firValueArguments += convertValueArguments(it)
                LAMBDA_ARGUMENT -> firValueArguments += convertAnnotatedLambda(it)
                else -> additionalArgument = getAsFirExpression(it)
            }
        }

        return FirFunctionCallImpl(session, null).apply {
            val calleeReference = when {
                name != null -> FirSimpleNamedReference(this@ExpressionsConverter.session, null, name.nameAsSafeName())
                additionalArgument != null -> {
                    arguments += additionalArgument!!
                    FirSimpleNamedReference(this@ExpressionsConverter.session, null, OperatorNameConventions.INVOKE)
                }
                else -> FirErrorNamedReference(this@ExpressionsConverter.session, null, "Call has no callee")
            }
            this.calleeReference = calleeReference
            FunctionUtil.firFunctionCalls += this
            this.extractArgumentsFrom(firValueArguments, stubMode)
            typeArguments += firTypeArguments
            FunctionUtil.firFunctionCalls.removeLast()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseAnnotatedLambda
     */
    private fun convertAnnotatedLambda(annotatedLambda: LighterASTNode): FirExpression {
        annotatedLambda.forEachChildren {
            when (it.tokenType) {
                LAMBDA_EXPRESSION -> ""
                LABELED_EXPRESSION -> ""
                ANNOTATED_EXPRESSION -> ""
            }
        }
        return FirExpressionStub(session, null)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseStringTemplate
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
        shortOrLongString.forEachChildren(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END) {
            firExpression = getAsFirExpression(it)
        }
        return firExpression
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLiteralConstant
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
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseArrayAccess
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitArrayAccessExpression
     */
    private fun convertArrayAccessExpression(arrayAccess: LighterASTNode): FirFunctionCall {
        lateinit var firExpression: FirExpression
        val indices: MutableList<FirExpression> = mutableListOf()
        arrayAccess.forEachChildren {
            when (it.tokenType) {
                INDICES -> indices += convertIndices(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }
        return FirFunctionCallImpl(session, null).apply {
            calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, OperatorNameConventions.GET)
            explicitReceiver = firExpression
            arguments += indices
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseAsCollectionLiteralExpression
     */
    private fun convertIndices(indices: LighterASTNode): List<FirExpression> {
        val firExpressionList: MutableList<FirExpression> = mutableListOf()
        indices.forEachChildren {
            if (it.isExpression()) firExpressionList += getAsFirExpression<FirExpression>(it)
        }

        return firExpressionList
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseSimpleNameExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitSimpleNameExpression
     */
    private fun convertSimpleNameExpression(referenceExpression: LighterASTNode): FirQualifiedAccessExpression {
        return FirQualifiedAccessExpressionImpl(session, null).apply {
            calleeReference =
                FirSimpleNamedReference(this@ExpressionsConverter.session, null, referenceExpression.getAsString().nameAsSafeName())
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThisExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThisExpression
     */
    private fun convertThisExpression(thisExpression: LighterASTNode): FirQualifiedAccessExpression {
        var label: String? = null
        thisExpression.forEachChildren {
            when (it.tokenType) {
                LABEL_QUALIFIER -> label = it.getAsString().replaceFirst("@", "")
            }
        }

        return FirQualifiedAccessExpressionImpl(session, null).apply {
            calleeReference = FirExplicitThisReference(this@ExpressionsConverter.session, null, label)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseSuperExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitSuperExpression
     */
    private fun convertSuperExpression(superExpression: LighterASTNode): FirQualifiedAccessExpression {
        var superTypeRef: FirTypeRef = implicitType
        superExpression.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> superTypeRef = declarationsConverter.convertType(it)
            }
        }

        return FirQualifiedAccessExpressionImpl(session, null).apply {
            calleeReference = FirExplicitSuperReference(this@ExpressionsConverter.session, null, superTypeRef)
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
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }
        return when {
            identifier != null -> FirNamedArgumentExpressionImpl(session, null, identifier.nameAsSafeName(), isSpread, firExpression)
            isSpread -> FirSpreadArgumentExpressionImpl(session, null, firExpression)
            else -> firExpression
        }
    }

    fun convertLValue(leftArgNode: LighterASTNode, container: FirModifiableQualifiedAccess<*>): FirReference {
        leftArgNode.forEachChildren {
            return when (it.tokenType) {
                THIS_EXPRESSION -> convertThisExpression(leftArgNode).calleeReference
                REFERENCE_EXPRESSION -> FirSimpleNamedReference(session, null, it.getAsString().nameAsSafeName())
                in qualifiedAccessTokens -> (getAsFirExpression(it) as FirQualifiedAccess).let { firQualifiedAccess ->
                    container.explicitReceiver = firQualifiedAccess.explicitReceiver
                    container.safe = firQualifiedAccess.safe
                    return@let firQualifiedAccess.calleeReference
                }
                PARENTHESIZED -> convertLValue(it.getExpressionInParentheses(), container)
                else -> FirErrorNamedReference(session, null, "Unsupported LValue: ${leftArgNode.tokenType}")
            }
        }
        return FirErrorNamedReference(session, null, "Unsupported LValue: ${leftArgNode.tokenType}")
    }
}