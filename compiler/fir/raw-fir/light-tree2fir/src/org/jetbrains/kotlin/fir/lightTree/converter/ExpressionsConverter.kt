/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.WhenEntry
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class ExpressionsConverter(
    session: FirSession,
    private val stubMode: Boolean,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val declarationsConverter: DeclarationsConverter,
    context: Context<LighterASTNode> = Context()
) : BaseConverter(session, tree, context) {
    override val offset: Int
        get() = declarationsConverter.offset

    inline fun <reified R : FirElement> getAsFirExpression(expression: LighterASTNode?, errorReason: String = ""): R {
        return expression?.let {
            convertExpression(it, errorReason)
        } as? R ?: buildErrorExpression(
            expression?.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
        ) as R
    }

    /*****    EXPRESSIONS    *****/
    fun convertExpression(expression: LighterASTNode, errorReason: String): FirElement {
        if (!stubMode) {
            return when (expression.tokenType) {
                LAMBDA_EXPRESSION -> {
                    val lambdaTree = LightTree2Fir.buildLightTreeLambdaExpression(expression.asText)
                    declarationsConverter.withOffset(offset + expression.startOffset) {
                        ExpressionsConverter(baseSession, stubMode, lambdaTree, declarationsConverter, context)
                            .convertLambdaExpression(lambdaTree.root)
                    }
                }
                BINARY_EXPRESSION -> convertBinaryExpression(expression)
                BINARY_WITH_TYPE -> convertBinaryWithTypeRHSExpression(expression) {
                    this.getOperationSymbol().toFirOperation()
                }
                IS_EXPRESSION -> convertBinaryWithTypeRHSExpression(expression) {
                    if (this == "is") FirOperation.IS else FirOperation.NOT_IS
                }
                LABELED_EXPRESSION -> convertLabeledExpression(expression)
                PREFIX_EXPRESSION, POSTFIX_EXPRESSION -> convertUnaryExpression(expression)
                ANNOTATED_EXPRESSION -> convertAnnotatedExpression(expression)
                CLASS_LITERAL_EXPRESSION -> convertClassLiteralExpression(expression)
                CALLABLE_REFERENCE_EXPRESSION -> convertCallableReferenceExpression(expression)
                in qualifiedAccessTokens -> convertQualifiedExpression(expression)
                CALL_EXPRESSION -> convertCallExpression(expression)
                WHEN -> convertWhenExpression(expression)
                ARRAY_ACCESS_EXPRESSION -> convertArrayAccessExpression(expression)
                COLLECTION_LITERAL_EXPRESSION -> convertCollectionLiteralExpression(expression)
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
                else -> buildErrorExpression(null, ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected))
            }
        }

        return buildExpressionStub()
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

        val expressionSource = lambdaExpression.toFirSourceElement()
        val target: FirFunctionTarget
        return buildAnonymousFunction {
            source = expressionSource
            session = baseSession
            origin = FirDeclarationOrigin.Source
            returnTypeRef = implicitType
            receiverTypeRef = implicitType
            symbol = FirAnonymousFunctionSymbol()
            isLambda = true
            label = context.firLabels.pop() ?: context.calleeNamesForLambda.lastOrNull()?.let {
                buildLabel { name = it.asString() }
            }
            target = FirFunctionTarget(labelName = label?.name, isLambda = true)
            context.firFunctionTargets += target
            val destructuringStatements = mutableListOf<FirStatement>()
            for (valueParameter in valueParameterList) {
                val multiDeclaration = valueParameter.destructuringDeclaration
                valueParameters += if (multiDeclaration != null) {
                    val name = DESTRUCTURING_NAME
                    val multiParameter = buildValueParameter {
                        source = valueParameter.firValueParameter.source
                        session = baseSession
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = valueParameter.firValueParameter.returnTypeRef
                        this.name = name
                        symbol = FirVariableSymbol(name)
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                    destructuringStatements += generateDestructuringBlock(
                        this@ExpressionsConverter.baseSession,
                        multiDeclaration,
                        multiParameter,
                        tmpVariable = false
                    ).statements
                    multiParameter
                } else {
                    valueParameter.firValueParameter
                }
            }

            body = if (block != null) {
                declarationsConverter.withOffset(expressionSource.startOffset) {
                    declarationsConverter.convertBlockExpressionWithoutBuilding(block!!).apply {
                        if (statements.isEmpty()) {
                            statements.add(
                                buildReturnExpression {
                                    source = expressionSource.fakeElement(FirFakeSourceElementKind.ImplicitReturn)
                                    this.target = target
                                    result = buildUnitExpression {
                                        source = expressionSource.fakeElement(FirFakeSourceElementKind.ImplicitUnit)
                                    }
                                }
                            )
                        }
                        statements.addAll(0, destructuringStatements)
                    }.build()
                }
            } else {
                buildSingleExpressionBlock(buildErrorExpression(null, ConeSimpleDiagnostic("Lambda has no body", DiagnosticKind.Syntax)))
            }
            context.firFunctionTargets.removeLast()
        }.also { 
            target.bind(it)
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
        var rightArg: LighterASTNode? = null
        var operationReferenceSource: FirLightSourceElement? = null
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    isLeftArgument = false
                    operationTokenName = it.asText
                    operationReferenceSource = it.toFirSourceElement()
                }
                else -> if (it.isExpression()) {
                    if (isLeftArgument) {
                        leftArgNode = it
                    } else {
                        rightArg = it
                    }
                }
            }
        }

        val baseSource = binaryExpression.toFirSourceElement()
        val operationToken = operationTokenName.getOperationSymbol()
        if (operationToken == IDENTIFIER) {
            context.calleeNamesForLambda += operationTokenName.nameAsSafeName()
        }

        val rightArgAsFir =
            if (rightArg != null)
                getAsFirExpression<FirExpression>(rightArg, "No right operand")
            else
                buildErrorExpression(null, ConeSimpleDiagnostic("No right operand", DiagnosticKind.Syntax))

        val leftArgAsFir = getAsFirExpression<FirExpression>(leftArgNode, "No left operand")

        if (operationToken == IDENTIFIER) {
            // No need for the callee name since arguments are already generated
            context.calleeNamesForLambda.removeLast()
        }

        when (operationToken) {
            ELVIS ->
                return leftArgAsFir.generateNotNullOrOther(rightArgAsFir, baseSource)
            ANDAND, OROR ->
                return leftArgAsFir.generateLazyLogicalOperation(rightArgAsFir, operationToken == ANDAND, baseSource)
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgAsFir.generateContainsOperation(leftArgAsFir, operationToken == NOT_IN, baseSource, operationReferenceSource)
            in OperatorConventions.COMPARISON_OPERATIONS ->
                return leftArgAsFir.generateComparisonExpression(rightArgAsFir, operationToken, baseSource, operationReferenceSource)
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            buildFunctionCall {
                source = binaryExpression.toFirSourceElement()
                calleeReference = buildSimpleNamedReference {
                    source = operationReferenceSource ?: this@buildFunctionCall.source
                    name = conventionCallName ?: operationTokenName.nameAsSafeName()
                }
                explicitReceiver = leftArgAsFir
                argumentList = buildUnaryArgumentList(rightArgAsFir)
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            if (firOperation in FirOperation.ASSIGNMENTS) {
                return leftArgNode.generateAssignment(binaryExpression.toFirSourceElement(), rightArg, rightArgAsFir, firOperation) { getAsFirExpression(this) }
            } else {
                buildEqualityOperatorCall {
                    source = binaryExpression.toFirSourceElement()
                    operation = firOperation
                    argumentList = buildBinaryArgumentList(leftArgAsFir, rightArgAsFir)
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.Precedence.parseRightHandSide
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitBinaryWithTypeRHSExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitIsExpression
     */
    private fun convertBinaryWithTypeRHSExpression(
        binaryExpression: LighterASTNode,
        toFirOperation: String.() -> FirOperation
    ): FirTypeOperatorCall {
        lateinit var operationTokenName: String
        var leftArgAsFir: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No left operand", DiagnosticKind.Syntax))
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.asText
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
            }
        }
        
        return buildTypeOperatorCall {
            source = binaryExpression.toFirSourceElement()
            operation = operationTokenName.toFirOperation()
            conversionTypeRef = firType
            argumentList = buildUnaryArgumentList(leftArgAsFir)
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
                LABEL_QUALIFIER -> context.firLabels += buildLabel { name = it.toString().replace("@", "") }
                BLOCK -> firExpression = declarationsConverter.convertBlock(it)
                PROPERTY -> firExpression = declarationsConverter.convertPropertyDeclaration(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        if (size != context.firLabels.size) {
            context.firLabels.removeLast()
            //println("Unused label: ${labeledExpression.getAsString()}")
        }
        return firExpression ?: buildErrorExpression(null, ConeSimpleDiagnostic("Empty label", DiagnosticKind.Syntax))
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitUnaryExpression
     */
    private fun convertUnaryExpression(unaryExpression: LighterASTNode): FirExpression {
        lateinit var operationTokenName: String
        var argument: LighterASTNode? = null
        var operationReference: LighterASTNode? = null
        unaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    operationReference = it
                    operationTokenName = it.asText
                }
                else -> if (it.isExpression()) argument = it
            }
        }

        val operationToken = operationTokenName.getOperationSymbol()
        val conventionCallName = operationToken.toUnaryName()
        return when {
            operationToken == EXCLEXCL -> {
                buildCheckNotNullCall {
                    source = unaryExpression.toFirSourceElement()
                    argumentList = buildUnaryArgumentList(getAsFirExpression<FirExpression>(argument, "No operand"))
                }

            }
            conventionCallName != null -> {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        unaryExpression,
                        operationReference,
                        argument,
                        callName = conventionCallName,
                        prefix = unaryExpression.tokenType == PREFIX_EXPRESSION
                    ) { getAsFirExpression(this) }
                }
                val receiver = getAsFirExpression<FirExpression>(argument, "No operand")
                if (operationToken == PLUS || operationToken == MINUS) {
                    if (receiver is FirConstExpression<*> && receiver.kind == ConstantValueKind.IntegerLiteral) {
                        val value = receiver.value as Long
                        val convertedValue = when (operationToken) {
                            MINUS -> -value
                            PLUS -> value
                            else -> error("Should not be here")
                        }
                        return buildConstExpression(unaryExpression.toFirSourceElement(), ConstantValueKind.IntegerLiteral, convertedValue)
                    }
                }
                buildFunctionCall {
                    source = unaryExpression.toFirSourceElement()
                    calleeReference = buildSimpleNamedReference {
                        source = this@buildFunctionCall.source
                        name = conventionCallName
                    }
                    explicitReceiver = receiver
                }
            }
            else -> throw IllegalStateException("Unexpected expression: ${unaryExpression.asText}")
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

        return firExpression?.also {
            require(it is FirAnnotationContainer)
            (it.annotations as MutableList<FirAnnotationCall>) += firAnnotationList
        } ?: buildErrorExpression(null, ConeSimpleDiagnostic("Strange annotated expression: ${firExpression?.render()}", DiagnosticKind.Syntax))
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitClassLiteralExpression
     */
    private fun convertClassLiteralExpression(classLiteralExpression: LighterASTNode): FirExpression {
        var firReceiverExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No receiver in class literal", DiagnosticKind.Syntax))
        classLiteralExpression.forEachChildren {
            if (it.isExpression()) firReceiverExpression = getAsFirExpression(it, "No receiver in class literal")
        }

        return buildGetClassCall {
            source = classLiteralExpression.toFirSourceElement()
            argumentList = buildUnaryArgumentList(firReceiverExpression)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitCallableReferenceExpression
     */
    private fun convertCallableReferenceExpression(callableReferenceExpression: LighterASTNode): FirExpression {
        var isReceiver = true
        var hasQuestionMarkAtLHS = false
        var firReceiverExpression: FirExpression? = null
        lateinit var firCallableReference: FirQualifiedAccess
        callableReferenceExpression.forEachChildren {
            when (it.tokenType) {
                COLONCOLON -> isReceiver = false
                QUEST -> hasQuestionMarkAtLHS = true
                else -> if (it.isExpression()) {
                    if (isReceiver) {
                        firReceiverExpression = getAsFirExpression(it, "Incorrect receiver expression")
                    } else {
                        firCallableReference = convertSimpleNameExpression(it)
                    }
                }
            }
        }

        return buildCallableReferenceAccess {
            source = callableReferenceExpression.toFirSourceElement()
            calleeReference = firCallableReference.calleeReference as FirNamedReference
            explicitReceiver = firReceiverExpression
            this.hasQuestionMarkAtLHS = hasQuestionMarkAtLHS
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitQualifiedExpression
     */
    private fun convertQualifiedExpression(dotQualifiedExpression: LighterASTNode): FirExpression {
        var isSelector = false
        var isSafe = false
        var firSelector: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax)) //after dot
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

        (firSelector as? FirQualifiedAccess)?.let {
            if (isSafe) {
                return it.wrapWithSafeCall(
                    firReceiver!!,
                    dotQualifiedExpression.toFirSourceElement(FirFakeSourceElementKind.DesugaredSafeCallExpression)
                )
            }

            it.replaceExplicitReceiver(firReceiver)

            @OptIn(FirImplementationDetail::class)
            it.replaceSource(dotQualifiedExpression.toFirSourceElement())
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
        var hasArguments = false
        var superNode: LighterASTNode? = null
        callSuffix.forEachChildren { child ->
            fun process(node: LighterASTNode) {
                when (node.tokenType) {
                    REFERENCE_EXPRESSION -> {
                        name = node.asText
                    }
                    SUPER_EXPRESSION -> {
                        superNode = node
                    }
                    PARENTHESIZED -> node.getExpressionInParentheses()?.let { process(it) } ?: run {
                        additionalArgument = getAsFirExpression(node, "Incorrect invoke receiver")
                    }
                    TYPE_ARGUMENT_LIST -> {
                        firTypeArguments += declarationsConverter.convertTypeArguments(node)
                    }
                    VALUE_ARGUMENT_LIST, LAMBDA_ARGUMENT -> {
                        hasArguments = true
                        valueArguments += node
                    }
                    else -> if (node.tokenType != TokenType.ERROR_ELEMENT) {
                        additionalArgument = getAsFirExpression(node, "Incorrect invoke receiver")
                    }
                }
            }

            process(child)
        }

        val source = callSuffix.toFirSourceElement()

        val (calleeReference, explicitReceiver, isImplicitInvoke) = when {
            name != null -> CalleeAndReceiver(
                buildSimpleNamedReference {
                    this.source = source
                    this.name = name.nameAsSafeName()
                }
            )

            additionalArgument != null -> {
                CalleeAndReceiver(
                    buildSimpleNamedReference {
                        this.source = source
                        this.name = OperatorNameConventions.INVOKE
                    },
                    additionalArgument!!,
                    isImplicitInvoke = true
                )
            }

            superNode != null -> {
                CalleeAndReceiver(
                    buildErrorNamedReference {
                        val node = superNode!!
                        this.source = node.toFirSourceElement()
                        diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed)
                    }
                )
            }

            else -> CalleeAndReceiver(
                buildErrorNamedReference {
                    this.source = source
                    diagnostic = ConeSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax)
                }
            )
        }

        val builder: FirQualifiedAccessBuilder = if (hasArguments) {
            val builder = if (isImplicitInvoke) FirImplicitInvokeCallBuilder() else FirFunctionCallBuilder()
            builder.apply {
                this.source = source
                this.calleeReference = calleeReference

                context.calleeNamesForLambda += calleeReference.name
                this.extractArgumentsFrom(valueArguments.flatMap { convertValueArguments(it) }, stubMode)
                context.calleeNamesForLambda.removeLast()
            }
        } else {
            FirQualifiedAccessExpressionBuilder().apply {
                this.source = source
                this.calleeReference = calleeReference
            }
        }
        return builder.apply {
            this.explicitReceiver = explicitReceiver
            typeArguments += firTypeArguments
        }.build() as FirExpression
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseStringTemplate
     */
    private fun convertStringTemplate(stringTemplate: LighterASTNode): FirExpression {
        return stringTemplate.getChildrenAsArray().toInterpolatingCall(stringTemplate) { convertShortOrLongStringTemplate(it) }
    }

    private fun LighterASTNode?.convertShortOrLongStringTemplate(errorReason: String): FirExpression {
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic(errorReason, DiagnosticKind.Syntax))
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
        val whenEntryNodes = mutableListOf<LighterASTNode>()
        val whenEntries = mutableListOf<WhenEntry>()
        whenExpression.forEachChildren {
            when (it.tokenType) {
                PROPERTY -> subjectVariable = (declarationsConverter.convertPropertyDeclaration(it) as FirVariable<*>).let { variable ->
                    buildProperty {
                        source = it.toFirSourceElement()
                        origin = FirDeclarationOrigin.Source
                        session = baseSession
                        returnTypeRef = variable.returnTypeRef
                        name = variable.name
                        initializer = variable.initializer
                        isVar = false
                        symbol = FirPropertySymbol(variable.name)
                        isLocal = true
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    }
                }
                DESTRUCTURING_DECLARATION -> subjectExpression =
                    getAsFirExpression(it, "Incorrect when subject expression: ${whenExpression.asText}")
                WHEN_ENTRY -> whenEntryNodes += it
                else -> if (it.isExpression()) subjectExpression =
                    getAsFirExpression(it, "Incorrect when subject expression: ${whenExpression.asText}")
            }
        }
        subjectExpression = subjectVariable?.initializer ?: subjectExpression
        val hasSubject = subjectExpression != null

        @OptIn(FirContractViolation::class)
        val subject = FirExpressionRef<FirWhenExpression>()
        whenEntryNodes.mapTo(whenEntries) { convertWhenEntry(it, subject.takeIf { hasSubject }) }
        return buildWhenExpression {
            source = whenExpression.toFirSourceElement()
            this.subject = subjectExpression
            this.subjectVariable = subjectVariable
            usedAsExpression = whenExpression.usedAsExpression
            for (entry in whenEntries) {
                val branch = entry.firBlock
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        val firCondition = entry.toFirWhenCondition()
                        buildWhenBranch {
                            source = branch.source
                            condition = firCondition
                            result = branch 
                        }
                    } else {
                        val firCondition = entry.toFirWhenConditionWithoutSubject()
                        buildWhenBranch {
                            source = branch.source
                            condition = firCondition
                            result = branch
                        }
                    }
                } else {
                    buildWhenBranch {
                        source = branch.source
                        condition = buildElseIfTrueCondition()
                        result = branch
                    }
                }
            }
        }.also {
            if (hasSubject) {
                subject.bind(it)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntry
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntryNotElse
     */
    private fun convertWhenEntry(whenEntry: LighterASTNode, whenRefWithSubject: FirExpressionRef<FirWhenExpression>?): WhenEntry {
        var isElse = false
        var firBlock: FirBlock = buildEmptyExpressionBlock()
        val conditions = mutableListOf<FirExpression>()
        whenEntry.forEachChildren {
            when (it.tokenType) {
                WHEN_CONDITION_EXPRESSION -> conditions += convertWhenConditionExpression(it, whenRefWithSubject)
                WHEN_CONDITION_IN_RANGE -> conditions += convertWhenConditionInRange(it, whenRefWithSubject)
                WHEN_CONDITION_IS_PATTERN -> conditions += convertWhenConditionIsPattern(it, whenRefWithSubject)
                ELSE_KEYWORD -> isElse = true
                BLOCK -> firBlock = declarationsConverter.convertBlock(it)
                else -> if (it.isExpression()) firBlock = declarationsConverter.convertBlock(it)
            }
        }

        return WhenEntry(conditions, firBlock, isElse)
    }

    private fun convertWhenConditionExpression(whenCondition: LighterASTNode, whenRefWithSubject: FirExpressionRef<FirWhenExpression>?): FirExpression {
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax))
        whenCondition.forEachChildren {
            when (it.tokenType) {
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No expression in condition with expression")
            }
        }
        return if (whenRefWithSubject != null) {
            buildEqualityOperatorCall {
                source = whenCondition.toFirSourceElement()
                operation = FirOperation.EQ
                argumentList = buildBinaryArgumentList(
                    buildWhenSubjectExpression {
                        whenRef = whenRefWithSubject
                    }, firExpression
                )
            }

        } else {
            firExpression
        }
    }

    private fun convertWhenConditionInRange(whenCondition: LighterASTNode, whenRefWithSubject: FirExpressionRef<FirWhenExpression>?): FirExpression {
        var isNegate = false
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No range in condition with range", DiagnosticKind.Syntax))
        var conditionSource: FirLightSourceElement? = null
        whenCondition.forEachChildren {
            when {
                it.tokenType == OPERATION_REFERENCE && it.asText == NOT_IN.value -> {
                    conditionSource = it.toFirSourceElement()
                    isNegate = true
                }
                it.tokenType == OPERATION_REFERENCE -> {
                    conditionSource = it.toFirSourceElement()
                }
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        val subjectExpression = if (whenRefWithSubject != null) {
            buildWhenSubjectExpression {
                whenRef = whenRefWithSubject
                source = whenCondition.toFirSourceElement()
            }
        } else {
            return buildErrorExpression {
                source = whenCondition.toFirSourceElement()
                diagnostic = ConeSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax)
            }
        }

        return firExpression.generateContainsOperation(
            subjectExpression,
            inverted = isNegate,
            baseSource = whenCondition.toFirSourceElement(),
            operationReferenceSource = conditionSource
        )
    }

    private fun convertWhenConditionIsPattern(whenCondition: LighterASTNode, whenRefWithSubject: FirExpressionRef<FirWhenExpression>?): FirExpression {
        lateinit var firOperation: FirOperation
        lateinit var firType: FirTypeRef
        whenCondition.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                IS_KEYWORD -> firOperation = FirOperation.IS
                NOT_IS -> firOperation = FirOperation.NOT_IS
            }
        }

        val subjectExpression = if (whenRefWithSubject != null) {
            buildWhenSubjectExpression {
                whenRef = whenRefWithSubject
            }
        } else {
            return buildErrorExpression {
                source = whenCondition.toFirSourceElement()
                diagnostic = ConeSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax)
            }
        }

        return buildTypeOperatorCall {
            source = whenCondition.toFirSourceElement()
            operation = firOperation
            conversionTypeRef = firType
            argumentList = buildUnaryArgumentList(subjectExpression)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseArrayAccess
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitArrayAccessExpression
     */
    private fun convertArrayAccessExpression(arrayAccess: LighterASTNode): FirFunctionCall {
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No array expression", DiagnosticKind.Syntax))
        val indices: MutableList<FirExpression> = mutableListOf()
        arrayAccess.forEachChildren {
            when (it.tokenType) {
                INDICES -> indices += convertIndices(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No array expression")
            }
        }
        val getArgument = context.arraySetArgument.remove(arrayAccess)
        return buildFunctionCall {
            val isGet = getArgument == null
            source = (if (isGet) arrayAccess else arrayAccess.getParent()!!).toFirSourceElement()
            calleeReference = buildSimpleNamedReference {
                source = arrayAccess.toFirSourceElement().fakeElement(FirFakeSourceElementKind.ArrayAccessNameReference)
                name = if (isGet) OperatorNameConventions.GET else OperatorNameConventions.SET
            }
            explicitReceiver = firExpression
            argumentList = buildArgumentList {
                arguments += indices
                getArgument?.let { arguments += it }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseCollectionLiteralExpression
     */
    private fun convertCollectionLiteralExpression(expression: LighterASTNode): FirExpression {
        val firExpressionList = mutableListOf<FirExpression>()
        expression.forEachChildren {
            if (it.isExpression()) firExpressionList += getAsFirExpression<FirExpression>(it, "Incorrect collection literal argument")
        }

        return buildArrayOfCall {
            source = expression.toFirSourceElement()
            argumentList = buildArgumentList {
                arguments += firExpressionList
            }
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
        return buildQualifiedAccessExpression {
            source = referenceExpression.toFirSourceElement()
            calleeReference = buildSimpleNamedReference {
                source = this@buildQualifiedAccessExpression.source
                name = referenceExpression.asText.nameAsSafeName()
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDoWhileExpression
     */
    private fun convertDoWhile(doWhileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression =
            buildErrorExpression(null, ConeSimpleDiagnostic("No condition in do-while loop", DiagnosticKind.Syntax))

        val target: FirLoopTarget
        return FirDoWhileLoopBuilder().apply {
            source = doWhileLoop.toFirSourceElement()
            // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
            target = prepareTarget()
            doWhileLoop.forEachChildren {
                when (it.tokenType) {
                    BODY -> block = it
                    CONDITION -> firCondition = getAsFirExpression(it, "No condition in do-while loop")
                }
            }
            condition = firCondition
        }.configure(target) { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitWhileExpression
     */
    private fun convertWhile(whileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No condition in while loop", DiagnosticKind.Syntax))
        whileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in while loop")
            }
        }

        val target: FirLoopTarget
        return FirWhileLoopBuilder().apply {
            source = whileLoop.toFirSourceElement()
            condition = firCondition
            // break/continue in the while loop condition will refer to an outer loop if any.
            // So, prepare the loop target after building the condition.
            target = prepareTarget()
        }.configure(target) { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFor
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitForExpression
     */
    private fun convertFor(forLoop: LighterASTNode): FirElement {
        var parameter: ValueParameter? = null
        var rangeExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("No range in for loop", DiagnosticKind.Syntax))
        var blockNode: LighterASTNode? = null
        forLoop.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> parameter = declarationsConverter.convertValueParameter(it)
                LOOP_RANGE -> rangeExpression = getAsFirExpression(it, "No range in for loop")
                BODY -> blockNode = it
            }
        }

        val fakeSource = forLoop.toFirSourceElement(FirFakeSourceElementKind.DesugaredForLoop)
        val target: FirLoopTarget
        // NB: FirForLoopChecker relies on this block existence and structure
        return buildBlock {
            source = fakeSource
            val iteratorVal = generateTemporaryVariable(
                this@ExpressionsConverter.baseSession,
                rangeExpression.source,
                ITERATOR_NAME,
                buildFunctionCall {
                    source = fakeSource
                    calleeReference = buildSimpleNamedReference {
                        source = fakeSource
                        name = OperatorNameConventions.ITERATOR
                    }
                    explicitReceiver = rangeExpression
                }
            )
            statements += iteratorVal
            statements += FirWhileLoopBuilder().apply {
                source = fakeSource
                condition = buildFunctionCall {
                    source = fakeSource
                    calleeReference = buildSimpleNamedReference {
                        source = fakeSource
                        name = OperatorNameConventions.HAS_NEXT
                    }
                    explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                }
                // break/continue in the for loop condition will refer to an outer loop if any.
                // So, prepare the loop target after building the condition.
                target = prepareTarget()
            }.configure(target) {
                // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                buildBlock block@{
                    source = blockNode?.toFirSourceElement()
                    statements += convertLoopBody(blockNode).statements
                    val valueParameter = parameter ?: return@block
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    val firLoopParameter = generateTemporaryVariable(
                        this@ExpressionsConverter.baseSession,
                        valueParameter.firValueParameter.source,
                        if (multiDeclaration != null) DESTRUCTURING_NAME else valueParameter.firValueParameter.name,
                        buildFunctionCall {
                            source = fakeSource
                            calleeReference = buildSimpleNamedReference {
                                source = fakeSource
                                name = OperatorNameConventions.NEXT
                            }
                            explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                        },
                        valueParameter.firValueParameter.returnTypeRef
                    )
                    if (multiDeclaration != null) {
                        val destructuringBlock = generateDestructuringBlock(
                            this@ExpressionsConverter.baseSession,
                            multiDeclaration,
                            firLoopParameter,
                            tmpVariable = true
                        )
                        statements.addAll(0, destructuringBlock.statements)
                    } else {
                        statements.add(0, firLoopParameter)
                    }

                }
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
            firStatement != null -> FirSingleExpressionBlock(firStatement!!)
            firBlock == null -> buildEmptyExpressionBlock()
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
        return buildTryExpression {
            source = tryExpression.toFirSourceElement()
            this.tryBlock = tryBlock
            this.finallyBlock = finallyBlock
            for ((parameter, block) in catchClauses) {
                if (parameter == null) continue
                catches += buildCatch {
                    this.parameter = parameter.firValueParameter
                    this.block = block
                }
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
        var firCondition: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("If statement should have condition", DiagnosticKind.Syntax))
        var thenBlock: LighterASTNode? = null
        var elseBlock: LighterASTNode? = null
        ifExpression.forEachChildren {
            when (it.tokenType) {
                CONDITION -> firCondition = getAsFirExpression(it, "If statement should have condition")
                THEN -> thenBlock = it
                ELSE -> elseBlock = it
            }
        }

        return buildWhenExpression {
            source = ifExpression.toFirSourceElement()
            val trueBranch = convertLoopBody(thenBlock)
            branches += buildWhenBranch {
                source = thenBlock?.toFirSourceElement()
                condition = firCondition
                result = trueBranch
            }
            if (elseBlock != null) {
                val elseBranch = convertLoopBody(elseBlock)
                branches += buildWhenBranch {
                    source = elseBlock?.toFirSourceElement()
                    condition = buildElseIfTrueCondition()
                    result = elseBranch
                }
            }
            usedAsExpression = ifExpression.usedAsExpression
        }
    }

    private val LighterASTNode.usedAsExpression: Boolean
        get() {
            var parent = getParent() ?: return true
            if (parent.elementType == ANNOTATED_EXPRESSION) {
                parent = parent.getParent() ?: return true
            }
            val parentTokenType = parent.tokenType
            if (parentTokenType == BLOCK) return false
            if (parentTokenType == THEN || parentTokenType == ELSE || parentTokenType == WHEN_ENTRY) {
                return parent.getParent()?.usedAsExpression ?: true
            }
            if (parentTokenType != BODY) return true
            val type = parent.getParent()?.tokenType ?: return true
            return !(type == FOR || type == WHILE || type == DO_WHILE)
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

        val jumpBuilder = if (isBreak) FirBreakExpressionBuilder() else FirContinueExpressionBuilder()
        val sourceElement = jump.toFirSourceElement()
        return jumpBuilder.apply {
            source = sourceElement
        }.bindLabel(jump).build()
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseReturn
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitReturnExpression
     */
    private fun convertReturn(returnExpression: LighterASTNode): FirExpression {
        var labelName: String? = null
        var firExpression: FirExpression = buildUnitExpression()
        returnExpression.forEachChildren {
            when (it.tokenType) {
                LABEL_QUALIFIER -> labelName = it.getAsStringWithoutBacktick().replace("@", "")
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "Incorrect return expression")
            }
        }

        return firExpression.toReturn(
            baseSource = returnExpression.toFirSourceElement(),
            labelName = labelName,
            fromKtReturnExpression = true
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThrow
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThrowExpression
     */
    private fun convertThrow(throwExpression: LighterASTNode): FirExpression {
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("Nothing to throw", DiagnosticKind.Syntax))
        throwExpression.forEachChildren {
            if (it.isExpression()) firExpression = getAsFirExpression(it, "Nothing to throw")
        }

        return buildThrowExpression {
            source = throwExpression.toFirSourceElement()
            exception = firExpression
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThisExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThisExpression
     */
    private fun convertThisExpression(thisExpression: LighterASTNode): FirQualifiedAccessExpression {
        val label: String? = thisExpression.getLabelName()
        return buildThisReceiverExpression {
            source = thisExpression.toFirSourceElement()
            calleeReference = buildExplicitThisReference { labelName = label }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseSuperExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitSuperExpression
     */
    private fun convertSuperExpression(superExpression: LighterASTNode): FirQualifiedAccessExpression {
        val label: String? = superExpression.getLabelName()
        var superTypeRef: FirTypeRef = implicitType
        superExpression.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> superTypeRef = declarationsConverter.convertType(it)
            }
        }

        return buildQualifiedAccessExpression {
            source = superExpression.toFirSourceElement()
            calleeReference = buildExplicitSuperReference {
                labelName = label
                this.superTypeRef = superTypeRef
            }
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
                ANNOTATED_EXPRESSION -> container += buildLambdaArgumentExpression {
                    source = valueArguments.toFirSourceElement()
                    expression = getAsFirExpression(node)
                }
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
        var firExpression: FirExpression = buildErrorExpression(null, ConeSimpleDiagnostic("Argument is absent", DiagnosticKind.Syntax))
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
            identifier != null -> buildNamedArgumentExpression {
                source = valueArgument.toFirSourceElement()
                expression = firExpression
                this.isSpread = isSpread
                name = identifier.nameAsSafeName()
            }
            isSpread -> buildSpreadArgumentExpression {
                source = valueArgument.toFirSourceElement()
                expression = firExpression
            }
            else -> firExpression
        }
    }
}
