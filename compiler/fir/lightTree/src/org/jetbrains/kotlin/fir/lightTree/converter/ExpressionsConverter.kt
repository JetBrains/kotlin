/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.java.PsiForStatementImpl
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorLoop
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.labels.FirLabelImpl
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.extractArgumentsFrom
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsStringWithoutBacktick
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.isExpression
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.converter.utils.generateDestructuringBlock
import org.jetbrains.kotlin.fir.lightTree.converter.utils.getOperationSymbol
import org.jetbrains.kotlin.fir.lightTree.converter.utils.qualifiedAccessTokens
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.WhenEntry
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class ExpressionsConverter(
    session: FirSession,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val declarationsConverter: DeclarationsConverter,
    context: Context = Context()
) : BaseConverter(session, tree, context) {

    inline fun <reified R : FirElement> LighterASTNode?.toFirExpression(errorReason: String = ""): R {
        return this?.let { convertExpression(it, errorReason) } as? R ?: (FirErrorExpressionImpl(session, null, errorReason) as R)
    }

    inline fun <reified R : FirElement> getAsFirExpression(expression: LighterASTNode?, errorReason: String = ""): R {
        return expression?.let { convertExpression(it, errorReason) } as? R ?: (FirErrorExpressionImpl(session, null, errorReason) as R)
    }

    /*****    EXPRESSIONS    *****/
    fun convertExpression(expression: LighterASTNode, errorReason: String): FirElement {
        if (!stubMode) {
            return when (expression.tokenType) {
                LAMBDA_EXPRESSION -> {
                    val lambdaTree = LightTree2Fir.buildLightTreeLambdaExpression(expression.asText)
                    ExpressionsConverter(session, stubMode, lambdaTree, declarationsConverter, context).convertLambdaExpression(lambdaTree.root)
                }
                BINARY_EXPRESSION -> convertBinaryExpression(expression)
                BINARY_WITH_TYPE -> convertBinaryWithType(expression)
                IS_EXPRESSION -> convertIsExpression(expression)
                LABELED_EXPRESSION -> convertLabeledExpression(expression)
                PREFIX_EXPRESSION, POSTFIX_EXPRESSION -> convertUnaryExpression(expression)
                ANNOTATED_EXPRESSION -> convertAnnotatedExpression(expression)
                CLASS_LITERAL_EXPRESSION -> convertClassLiteralExpression(expression)
                CALLABLE_REFERENCE_EXPRESSION -> convertCallableReferenceExpression(expression)
                in qualifiedAccessTokens -> convertQualifiedExpression(expression)
                CALL_EXPRESSION -> convertCallExpression(expression)
                WHEN -> convertWhenExpression(expression)
                ARRAY_ACCESS_EXPRESSION -> convertArrayAccessExpression(expression)
                COLLECTION_LITERAL_EXPRESSION -> convertCollectionLiteralExpresion(expression)
                STRING_TEMPLATE -> convertStringTemplate(expression)
                is KtConstantExpressionElementType -> convertConstantExpression(expression)
                REFERENCE_EXPRESSION -> convertSimpleNameExpression(expression)
                DO_WHILE -> convertDoWhile(expression)
                WHILE -> convertWhile(expression)
                FOR -> convertFor(expression)
                TRY -> convertTryExpression(expression)
                IF -> convertIfExpression(expression)
                BREAK, CONTINUE -> convertLoopJump(expression)
                RETURN -> convertReturn(expression)
                THROW -> convertThrow(expression)
                PARENTHESIZED -> getAsFirExpression(expression.getExpressionInParentheses(), "Empty parentheses")
                PROPERTY_DELEGATE, INDICES, CONDITION, LOOP_RANGE ->
                    getAsFirExpression(expression.getExpressionInParentheses(), errorReason)
                THIS_EXPRESSION -> convertThisExpression(expression)
                SUPER_EXPRESSION -> convertSuperExpression(expression)

                OBJECT_LITERAL -> declarationsConverter.convertObjectLiteral(expression)
                FUN -> declarationsConverter.convertFunctionDeclaration(expression)
                else -> FirErrorExpressionImpl(session, null, errorReason)
            }
        }

        return FirExpressionStub(session, null)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFunctionLiteral
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitLambdaExpression
     */
    private fun convertLambdaExpression(lambdaExpression: LighterASTNode): FirExpression {
        val valueParameterList = mutableListOf<ValueParameter>()
        var block: LighterASTNode? = null
        lambdaExpression.getChildNodesByType(FUNCTION_LITERAL).first().forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER_LIST -> valueParameterList += declarationsConverter.convertValueParameters(it)
                BLOCK -> block = it
            }
        }

        return FirAnonymousFunctionImpl(session, null, implicitType, implicitType).apply {
            context.firFunctions += this
            var destructuringBlock: FirExpression? = null
            for (valueParameter in valueParameterList) {
                val multiDeclaration = valueParameter.destructuringDeclaration
                valueParameters += if (multiDeclaration != null) {
                    val multiParameter = FirValueParameterImpl(
                        this@ExpressionsConverter.session, null, Name.special("<destruct>"),
                        FirImplicitTypeRefImpl(this@ExpressionsConverter.session, null),
                        defaultValue = null, isCrossinline = false, isNoinline = false, isVararg = false
                    )
                    destructuringBlock = generateDestructuringBlock(
                        this@ExpressionsConverter.session,
                        multiDeclaration,
                        multiParameter,
                        tmpVariable = false
                    )
                    multiParameter
                } else {
                    valueParameter.firValueParameter
                }
            }
            label = context.firLabels.pop() ?: context.firFunctionCalls.lastOrNull()?.calleeReference?.name?.let {
                FirLabelImpl(this@ExpressionsConverter.session, null, it.asString())
            }
            val bodyExpression = block?.let { declarationsConverter.convertBlockExpression(it) }
                ?: FirErrorExpressionImpl(this@ExpressionsConverter.session, null, "Lambda has no body")
            body = if (bodyExpression is FirBlockImpl) {
                if (bodyExpression.statements.isEmpty()) {
                    bodyExpression.statements.add(FirUnitExpression(this@ExpressionsConverter.session, null))
                }
                if (destructuringBlock is FirBlock) {
                    for ((index, statement) in destructuringBlock.statements.withIndex()) {
                        bodyExpression.statements.add(index, statement)
                    }
                }
                bodyExpression
            } else {
                FirSingleExpressionBlock(this@ExpressionsConverter.session, bodyExpression.toReturn())
            }

            context.firFunctions.removeLast()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseBinaryExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitBinaryExpression
     */
    private fun convertBinaryExpression(binaryExpression: LighterASTNode): FirStatement {
        var isLeftArgument = true
        lateinit var operationTokenName: String
        var leftArgNode: LighterASTNode? = null
        var rightArgAsFir: FirExpression = FirErrorExpressionImpl(session, null, "No right operand")
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    isLeftArgument = false
                    operationTokenName = it.asText
                }
                else -> if (it.isExpression()) {
                    if (isLeftArgument) {
                        leftArgNode = it
                    } else {
                        rightArgAsFir = getAsFirExpression(it, "No right operand")
                    }
                }
            }
        }

        val operationToken = operationTokenName.getOperationSymbol()
        when (operationToken) {
            ELVIS ->
                return getAsFirExpression<FirExpression>(leftArgNode, "No left operand").generateNotNullOrOther(
                    session, rightArgAsFir, "elvis", null
                )
            ANDAND, OROR ->
                return getAsFirExpression<FirExpression>(leftArgNode, "No left operand").generateLazyLogicalOperation(
                    session, rightArgAsFir, operationToken == ANDAND, null
                )
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgAsFir.generateContainsOperation(
                    session, getAsFirExpression(leftArgNode, "No left operand"), operationToken == NOT_IN, null, null
                )
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(
                    this@ExpressionsConverter.session, null,
                    conventionCallName ?: operationTokenName.nameAsSafeName()
                )
                explicitReceiver = getAsFirExpression(leftArgNode, "No left operand")
                arguments += rightArgAsFir
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            if (firOperation in FirOperation.ASSIGNMENTS) {
                return leftArgNode.generateAssignment(null, rightArgAsFir, firOperation) { toFirExpression() }
            } else {
                FirOperatorCallImpl(session, null, firOperation).apply {
                    arguments += getAsFirExpression<FirExpression>(leftArgNode, "No left operand")
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
        var leftArgAsFir: FirExpression = FirErrorExpressionImpl(session, null, "No left operand")
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.asText
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
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
        var leftArgAsFir: FirExpression = FirErrorExpressionImpl(session, null, "No left operand")
        lateinit var firType: FirTypeRef
        isExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.asText
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
            }
        }

        val operation = if (operationTokenName == "is") FirOperation.IS else FirOperation.NOT_IS
        return FirTypeOperatorCallImpl(session, null, operation, firType).apply {
            arguments += leftArgAsFir
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLabeledExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitLabeledExpression
     */
    private fun convertLabeledExpression(labeledExpression: LighterASTNode): FirElement {
        val size = context.firLabels.size
        var firExpression: FirElement? = null
        labeledExpression.forEachChildren {
            when (it.tokenType) {
                LABEL_QUALIFIER -> context.firLabels += FirLabelImpl(session, null, it.toString().replace("@", ""))
                BLOCK -> firExpression = declarationsConverter.convertBlock(it)
                PROPERTY -> firExpression = declarationsConverter.convertPropertyDeclaration(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        if (size != context.firLabels.size) {
            context.firLabels.removeLast()
            //println("Unused label: ${labeledExpression.getAsString()}")
        }
        return firExpression ?: FirErrorExpressionImpl(session, null, "Empty label")
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitUnaryExpression
     */
    private fun convertUnaryExpression(unaryExpression: LighterASTNode): FirExpression {
        lateinit var operationTokenName: String
        var argument: LighterASTNode? = null
        unaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.asText
                else -> if (it.isExpression()) argument = it
            }
        }

        val operationToken = operationTokenName.getOperationSymbol()
        if (operationToken == EXCLEXCL) {
            return argument.bangBangToWhen(null) { toFirExpression(it) }
        }

        val conventionCallName = operationToken.toUnaryName()
        return if (conventionCallName != null) {
            if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                return generateIncrementOrDecrementBlock(
                    null,
                    argument,
                    callName = conventionCallName,
                    prefix = unaryExpression.tokenType == PREFIX_EXPRESSION
                ) { toFirExpression() }
            }
            FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, conventionCallName)
                explicitReceiver = getAsFirExpression(argument, "No operand")
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            FirOperatorCallImpl(session, null, firOperation).apply {
                arguments += getAsFirExpression<FirExpression>(argument, "No operand")
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitAnnotatedExpression
     */
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
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitClassLiteralExpression
     */
    private fun convertClassLiteralExpression(classLiteralExpression: LighterASTNode): FirExpression {
        var firReceiverExpression: FirExpression = FirErrorExpressionImpl(session, null, "No receiver in class literal")
        classLiteralExpression.forEachChildren {
            if (it.isExpression()) firReceiverExpression = getAsFirExpression(it, "No receiver in class literal")
        }

        return FirGetClassCallImpl(session, null).apply {
            arguments += firReceiverExpression
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitCallableReferenceExpression
     */
    private fun convertCallableReferenceExpression(callableReferenceExpression: LighterASTNode): FirExpression {
        var isReceiver = true
        var firReceiverExpression: FirExpression? = null
        lateinit var firCallableReference: FirQualifiedAccess
        callableReferenceExpression.forEachChildren {
            when (it.tokenType) {
                COLONCOLON -> isReceiver = false
                else -> if (it.isExpression()) {
                    if (isReceiver) {
                        firReceiverExpression = getAsFirExpression(it, "Incorrect receiver expression")
                    } else {
                        firCallableReference = convertSimpleNameExpression(it)
                    }
                }
            }
        }

        return FirCallableReferenceAccessImpl(session, null).apply {
            calleeReference = firCallableReference.calleeReference
            explicitReceiver = firReceiverExpression
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitQualifiedExpression
     */
    private fun convertQualifiedExpression(dotQualifiedExpression: LighterASTNode): FirExpression {
        var isSelector = false
        var isSafe = false
        var firSelector: FirExpression = FirErrorExpressionImpl(session, null, "Qualified expression without selector") //after dot
        var firReceiver: FirExpression? = null //before dot
        dotQualifiedExpression.forEachChildren {
            when (it.tokenType) {
                DOT -> isSelector = true
                SAFE_ACCESS -> {
                    isSafe = true
                    isSelector = true
                }
                else -> {
                    if (isSelector && it.tokenType != TokenType.ERROR_ELEMENT)
                        firSelector = getAsFirExpression(it, "Incorrect selector expression")
                    else
                        firReceiver = getAsFirExpression(it, "Incorrect receiver expression")
                }
            }
        }

        //TODO use contracts?
        if (firSelector is FirModifiableQualifiedAccess<*>) {
            (firSelector as FirModifiableQualifiedAccess<*>).safe = isSafe
            (firSelector as FirModifiableQualifiedAccess<*>).explicitReceiver = firReceiver
        }
        return firSelector
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseCallSuffix
     */
    private fun convertCallExpression(callSuffix: LighterASTNode): FirExpression {
        var name: String? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        val valueArguments = mutableListOf<LighterASTNode>()
        var additionalArgument: FirExpression? = null
        callSuffix.forEachChildren {
            when (it.tokenType) {
                REFERENCE_EXPRESSION -> name = it.asText
                TYPE_ARGUMENT_LIST -> firTypeArguments += declarationsConverter.convertTypeArguments(it)
                VALUE_ARGUMENT_LIST, LAMBDA_ARGUMENT -> valueArguments += it
                else -> if (it.tokenType != TokenType.ERROR_ELEMENT) additionalArgument =
                    getAsFirExpression(it, "Incorrect invoke receiver")
            }
        }

        return FirFunctionCallImpl(session, null).apply {
            this.calleeReference = when {
                name != null -> FirSimpleNamedReference(this@ExpressionsConverter.session, null, name.nameAsSafeName())
                additionalArgument != null -> {
                    arguments += additionalArgument!!
                    FirSimpleNamedReference(this@ExpressionsConverter.session, null, OperatorNameConventions.INVOKE)
                }
                else -> FirErrorNamedReference(this@ExpressionsConverter.session, null, "Call has no callee")
            }

            context.firFunctionCalls += this
            this.extractArgumentsFrom(valueArguments.flatMap { convertValueArguments(it) }, stubMode)
            typeArguments += firTypeArguments
            context.firFunctionCalls.removeLast()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseStringTemplate
     */
    private fun convertStringTemplate(stringTemplate: LighterASTNode): FirExpression {
        return stringTemplate.getChildrenAsArray().toInterpolatingCall(null) { convertShortOrLongStringTemplate(it) }
    }

    private fun LighterASTNode?.convertShortOrLongStringTemplate(errorReason: String): FirExpression {
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, errorReason)
        this?.forEachChildren(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END) {
            firExpression = getAsFirExpression(it, errorReason)
        }
        return firExpression
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLiteralConstant
     */
    private fun convertConstantExpression(constantExpression: LighterASTNode): FirExpression {
        return generateConstantExpressionByLiteral(constantExpression)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhen
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitWhenExpression
     */
    private fun convertWhenExpression(whenExpression: LighterASTNode): FirExpression {
        var subjectExpression: FirExpression? = null
        var subjectVariable: FirVariable<*>? = null
        val whenEntries = mutableListOf<WhenEntry>()
        whenExpression.forEachChildren {
            when (it.tokenType) {
                PROPERTY -> subjectVariable = (declarationsConverter.convertPropertyDeclaration(it) as FirVariable<*>).let { variable ->
                    FirVariableImpl(
                        session, null, variable.name, variable.returnTypeRef,
                        isVar = false, initializer = variable.initializer
                    )
                }
                DESTRUCTURING_DECLARATION -> subjectExpression =
                    getAsFirExpression(it, "Incorrect when subject expression: ${whenExpression.asText}")
                WHEN_ENTRY -> whenEntries += convertWhenEntry(it)
                else -> if (it.isExpression()) subjectExpression =
                    getAsFirExpression(it, "Incorrect when subject expression: ${whenExpression.asText}")
            }
        }

        subjectExpression = subjectVariable?.initializer ?: subjectExpression
        val hasSubject = subjectExpression != null
        val subject = FirWhenSubject()
        return FirWhenExpressionImpl(session, null, subjectExpression, subjectVariable).apply {
            if (hasSubject) {
                subject.bind(this)
            }
            for (entry in whenEntries) {
                val branch = entry.firBlock
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        val firCondition = entry.toFirWhenCondition(this@ExpressionsConverter.session, subject)
                        FirWhenBranchImpl(this@ExpressionsConverter.session, null, firCondition, branch)
                    } else {
                        val firCondition = entry.toFirWhenConditionWithoutSubject(this@ExpressionsConverter.session)
                        FirWhenBranchImpl(this@ExpressionsConverter.session, null, firCondition, branch)
                    }
                } else {
                    FirWhenBranchImpl(
                        this@ExpressionsConverter.session, null, FirElseIfTrueCondition(this@ExpressionsConverter.session, null), branch
                    )
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntry
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntryNotElse
     */
    private fun convertWhenEntry(whenEntry: LighterASTNode): WhenEntry {
        var isElse = false
        var firBlock: FirBlock = FirEmptyExpressionBlock(session)
        val conditions = mutableListOf<FirExpression>()
        whenEntry.forEachChildren {
            when (it.tokenType) {
                WHEN_CONDITION_EXPRESSION -> conditions += convertWhenConditionExpression(it)
                WHEN_CONDITION_IN_RANGE -> conditions += convertWhenConditionInRange(it)
                WHEN_CONDITION_IS_PATTERN -> conditions += convertWhenConditionIsPattern(it)
                ELSE_KEYWORD -> isElse = true
                BLOCK -> firBlock = declarationsConverter.convertBlock(it)
                else -> if (it.isExpression()) firBlock = declarationsConverter.convertBlock(it)
            }
        }

        return WhenEntry(conditions, firBlock, isElse)
    }

    private fun convertWhenConditionExpression(whenCondition: LighterASTNode): FirExpression {
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "No expression in condition with expression")
        whenCondition.forEachChildren {
            when (it.tokenType) {
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No expression in condition with expression")
            }
        }

        return FirOperatorCallImpl(session, null, FirOperation.EQ).apply {
            arguments += firExpression
        }
    }

    private fun convertWhenConditionInRange(whenCondition: LighterASTNode): FirExpression {
        var isNegate = false
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "No range in condition with range")
        whenCondition.forEachChildren {
            when (it.tokenType) {
                NOT_IN -> isNegate = true
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        val name = if (isNegate) OperatorNameConventions.NOT else SpecialNames.NO_NAME_PROVIDED
        return FirFunctionCallImpl(session, null).apply {
            calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, name)
            explicitReceiver = firExpression
        }
    }

    private fun convertWhenConditionIsPattern(whenCondition: LighterASTNode): FirExpression {
        lateinit var firOperation: FirOperation
        lateinit var firType: FirTypeRef
        whenCondition.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                IS_KEYWORD -> firOperation = FirOperation.IS
                NOT_IS -> firOperation = FirOperation.NOT_IS
            }
        }

        return FirTypeOperatorCallImpl(session, null, firOperation, firType)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseArrayAccess
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitArrayAccessExpression
     */
    private fun convertArrayAccessExpression(arrayAccess: LighterASTNode): FirFunctionCall {
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "No array expression")
        val indices: MutableList<FirExpression> = mutableListOf()
        arrayAccess.forEachChildren {
            when (it.tokenType) {
                INDICES -> indices += convertIndices(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No array expression")
            }
        }
        return FirFunctionCallImpl(session, null).apply {
            calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, OperatorNameConventions.GET)
            explicitReceiver = firExpression
            arguments += indices
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseCollectionLiteralExpression
     */
    private fun convertCollectionLiteralExpresion(expression: LighterASTNode): FirExpression {
        val firExpressionList = mutableListOf<FirExpression>()
        expression.forEachChildren {
            if (it.isExpression()) firExpressionList += getAsFirExpression<FirExpression>(it, "Incorrect collection literal argument")
        }

        return FirArrayOfCallImpl(session, null).apply {
            arguments += firExpressionList
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseAsCollectionLiteralExpression
     */
    private fun convertIndices(indices: LighterASTNode): List<FirExpression> {
        val firExpressionList: MutableList<FirExpression> = mutableListOf()
        indices.forEachChildren {
            if (it.isExpression()) firExpressionList += getAsFirExpression<FirExpression>(it, "Incorrect index expression")
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
                FirSimpleNamedReference(this@ExpressionsConverter.session, null, referenceExpression.asText.nameAsSafeName())
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDoWhileExpression
     */
    private fun convertDoWhile(doWhileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression = FirErrorExpressionImpl(session, null, "No condition in do-while loop")
        doWhileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in do-while loop")
            }
        }

        return FirDoWhileLoopImpl(session, null, firCondition).configure { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitWhileExpression
     */
    private fun convertWhile(whileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression = FirErrorExpressionImpl(session, null, "No condition in while loop")
        whileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in while loop")
            }
        }

        return FirWhileLoopImpl(session, null, firCondition).configure { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFor
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitForExpression
     */
    private fun convertFor(forLoop: LighterASTNode): FirElement {
        var parameter: ValueParameter? = null
        var rangeExpression: FirExpression = FirErrorExpressionImpl(session, null, "No range in for loop")
        var blockNode: LighterASTNode? = null
        forLoop.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> parameter = declarationsConverter.convertValueParameter(it)
                LOOP_RANGE -> rangeExpression = getAsFirExpression(it, "No range in for loop")
                BODY -> blockNode = it
            }
        }

        //TODO psi must be null
        return FirBlockImpl(session, KtForExpression(PsiForStatementImpl())).apply {
            val rangeVal =
                generateTemporaryVariable(this@ExpressionsConverter.session, null, Name.special("<range>"), rangeExpression)
            statements += rangeVal
            val iteratorVal = generateTemporaryVariable(
                this@ExpressionsConverter.session, null, Name.special("<iterator>"),
                FirFunctionCallImpl(this@ExpressionsConverter.session, null).apply {
                    calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, Name.identifier("iterator"))
                    explicitReceiver = generateResolvedAccessExpression(this@ExpressionsConverter.session, null, rangeVal)
                }
            )
            statements += iteratorVal
            statements += FirWhileLoopImpl(
                this@ExpressionsConverter.session, null,
                FirFunctionCallImpl(this@ExpressionsConverter.session, null).apply {
                    calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, Name.identifier("hasNext"))
                    explicitReceiver = generateResolvedAccessExpression(this@ExpressionsConverter.session, null, iteratorVal)
                }
            ).configure {
                // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                val block = FirBlockImpl(this@ExpressionsConverter.session, null).apply {
                    statements += convertLoopBody(blockNode).statements
                }
                if (parameter != null) {
                    val multiDeclaration = parameter!!.destructuringDeclaration
                    val firLoopParameter = generateTemporaryVariable(
                        this@ExpressionsConverter.session, null,
                        if (multiDeclaration != null) Name.special("<destruct>") else parameter!!.firValueParameter.name,
                        FirFunctionCallImpl(this@ExpressionsConverter.session, null).apply {
                            calleeReference = FirSimpleNamedReference(this@ExpressionsConverter.session, null, Name.identifier("next"))
                            explicitReceiver = generateResolvedAccessExpression(this@ExpressionsConverter.session, null, iteratorVal)
                        }
                    )
                    if (multiDeclaration != null) {
                        val destructuringBlock = generateDestructuringBlock(
                            this@ExpressionsConverter.session,
                            multiDeclaration,
                            firLoopParameter,
                            tmpVariable = true
                        )
                        if (destructuringBlock is FirBlock) {
                            for ((index, statement) in destructuringBlock.statements.withIndex()) {
                                block.statements.add(index, statement)
                            }
                        }
                    } else {
                        block.statements.add(0, firLoopParameter)
                    }
                }
                block
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLoopBody
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.toFirBlock
     */
    private fun convertLoopBody(body: LighterASTNode?): FirBlock {
        var firBlock: FirBlock? = null
        var firStatement: FirStatement? = null
        body?.forEachChildren {
            when (it.tokenType) {
                BLOCK -> firBlock = declarationsConverter.convertBlockExpression(it)
                else -> if (it.isExpression()) firStatement = getAsFirExpression(it)
            }
        }

        return when {
            firStatement != null -> FirSingleExpressionBlock(session, firStatement!!)
            firBlock == null -> FirEmptyExpressionBlock(session)
            else -> firBlock!!
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitTryExpression
     */
    private fun convertTryExpression(tryExpression: LighterASTNode): FirExpression {
        lateinit var tryBlock: FirBlock
        val catchClauses = mutableListOf<Pair<ValueParameter?, FirBlock>>()
        var finallyBlock: FirBlock? = null
        tryExpression.forEachChildren {
            when (it.tokenType) {
                BLOCK -> tryBlock = declarationsConverter.convertBlock(it)
                CATCH -> convertCatchClause(it)?.also { oneClause -> catchClauses += oneClause }
                FINALLY -> finallyBlock = convertFinally(it)
            }
        }
        return FirTryExpressionImpl(session, null, tryBlock, finallyBlock).apply {
            for ((parameter, block) in catchClauses) {
                if (parameter == null) continue
                catches += FirCatchImpl(this@ExpressionsConverter.session, null, parameter.firValueParameter, block)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     */
    private fun convertCatchClause(catchClause: LighterASTNode): Pair<ValueParameter?, FirBlock>? {
        var valueParameter: ValueParameter? = null
        var blockNode: LighterASTNode? = null
        catchClause.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER_LIST -> valueParameter = declarationsConverter.convertValueParameters(it).firstOrNull() ?: return null
                BLOCK -> blockNode = it
            }
        }

        return Pair(valueParameter, declarationsConverter.convertBlock(blockNode))
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     */
    private fun convertFinally(finallyExpression: LighterASTNode): FirBlock {
        var blockNode: LighterASTNode? = null
        finallyExpression.forEachChildren {
            when (it.tokenType) {
                BLOCK -> blockNode = it
            }
        }

        return declarationsConverter.convertBlock(blockNode)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseIf
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitIfExpression
     */
    private fun convertIfExpression(ifExpression: LighterASTNode): FirExpression {
        var firCondition: FirExpression = FirErrorExpressionImpl(session, null, "If statement should have condition")
        var thenBlock: LighterASTNode? = null
        var elseBlock: LighterASTNode? = null
        ifExpression.forEachChildren {
            when (it.tokenType) {
                CONDITION -> firCondition = getAsFirExpression(it, "If statement should have condition")
                THEN -> thenBlock = it
                ELSE -> elseBlock = it
            }
        }

        return FirWhenExpressionImpl(session, null).apply {
            val trueBranch = convertLoopBody(thenBlock)
            branches += FirWhenBranchImpl(this@ExpressionsConverter.session, null, firCondition, trueBranch)
            val elseBranch = convertLoopBody(elseBlock)
            branches += FirWhenBranchImpl(
                this@ExpressionsConverter.session, null, FirElseIfTrueCondition(this@ExpressionsConverter.session, null), elseBranch
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseJump
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitBreakExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitContinueExpression
     */
    private fun convertLoopJump(jump: LighterASTNode): FirExpression {
        var isBreak = true
        jump.forEachChildren {
            when (it.tokenType) {
                CONTINUE_KEYWORD -> isBreak = false
                //BREAK -> isBreak = true
            }
        }

        return (if (isBreak) FirBreakExpressionImpl(session, null) else FirContinueExpressionImpl(session, null)).bindLabel(jump)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseReturn
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitReturnExpression
     */
    private fun convertReturn(returnExpression: LighterASTNode): FirExpression {
        var labelName: String? = null
        var firExpression: FirExpression = FirUnitExpression(session, null)
        returnExpression.forEachChildren {
            when (it.tokenType) {
                LABEL_QUALIFIER -> labelName = it.getAsStringWithoutBacktick().replace("@", "")
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "Incorrect return expression")
            }
        }

        return firExpression.toReturn(labelName = labelName)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThrow
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThrowExpression
     */
    private fun convertThrow(throwExpression: LighterASTNode): FirExpression {
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "Nothing to throw")
        throwExpression.forEachChildren {
            if (it.isExpression()) firExpression = getAsFirExpression(it, "Nothing to throw")
        }

        return FirThrowExpressionImpl(session, null, firExpression)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThisExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThisExpression
     */
    private fun convertThisExpression(thisExpression: LighterASTNode): FirQualifiedAccessExpression {
        val label: String? = thisExpression.getLabelName()

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
                LAMBDA_EXPRESSION,
                LABELED_EXPRESSION,
                ANNOTATED_EXPRESSION -> container += FirLambdaArgumentExpressionImpl(session, null, getAsFirExpression(node))
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
        var firExpression: FirExpression = FirErrorExpressionImpl(session, null, "Argument is absent")
        valueArgument.forEachChildren {
            when (it.tokenType) {
                VALUE_ARGUMENT_NAME -> identifier = it.asText
                MUL -> isSpread = true
                STRING_TEMPLATE -> firExpression = convertStringTemplate(it)
                is KtConstantExpressionElementType -> firExpression = convertConstantExpression(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "Argument is absent")
            }
        }
        return when {
            identifier != null -> FirNamedArgumentExpressionImpl(session, null, identifier.nameAsSafeName(), isSpread, firExpression)
            isSpread -> FirSpreadArgumentExpressionImpl(session, null, firExpression)
            else -> firExpression
        }
    }
}