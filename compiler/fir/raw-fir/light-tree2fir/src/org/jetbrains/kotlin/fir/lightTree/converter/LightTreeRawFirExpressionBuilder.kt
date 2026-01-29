/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("KDocUnresolvedReference", "KDocUnresolvedReference")

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.ElementTypeUtils.getOperationSymbol
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.WhenEntry
import org.jetbrains.kotlin.fir.lightTree.fir.addDestructuringStatements
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.buildErrorNamedReferenceWithNoName
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtNameReferenceExpressionElementType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class LightTreeRawFirExpressionBuilder(
    session: FirSession,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val declarationBuilder: LightTreeRawFirDeclarationBuilder,
    context: Context<LighterASTNode> = Context(),
) : AbstractLightTreeRawFirBuilder(session, tree, context) {

    internal inline fun <reified R : FirExpression> getAsFirExpression(
        expression: LighterASTNode,
        errorReason: String = "",
        isValidExpression: (R) -> Boolean = { !it.isStatementLikeExpression },
    ): R {
        return getAsFirExpression(expression, errorReason, expression, isValidExpression)
    }

    internal inline fun <reified R : FirExpression> getAsFirExpression(
        expression: LighterASTNode?,
        errorReason: String = "",
        sourceWhenInvalidExpression: LighterASTNode,
        isValidExpression: (R) -> Boolean = { !it.isStatementLikeExpression },
    ): R {
        val converted = expression?.let { getAsFirStatement(it, errorReason) }

        return wrapExpressionIfNeeded(expression, converted, isValidExpression, sourceWhenInvalidExpression, errorReason)
    }

    private inline fun <reified R : FirExpression> wrapExpressionIfNeeded(
        expression: LighterASTNode?,
        converted: FirElement?,
        isValidExpression: (R) -> Boolean,
        sourceWhenInvalidExpression: LighterASTNode,
        errorReason: String,
    ): R {
        return when {
            converted is R -> when {
                isValidExpression(converted) -> converted
                else -> buildErrorExpression(
                    source = converted.source?.realElement()
                        ?: expression?.toFirSourceElement()
                        ?: sourceWhenInvalidExpression.toFirSourceElement(kind = KtFakeSourceElementKind.ErrorExpression),
                    diagnostic = ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected),
                    element = converted,
                )
            }
            else -> buildErrorExpression(
                source = converted?.source?.realElement()
                    ?: expression?.toFirSourceElement()
                    ?: sourceWhenInvalidExpression.toFirSourceElement(kind = KtFakeSourceElementKind.ErrorExpression),
                diagnostic = if (expression == null) {
                    ConeSyntaxDiagnostic(errorReason)
                } else {
                    ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
                },
                element = converted,
            )
        } as R
    }

    /*****    EXPRESSIONS    *****/
    fun getAsFirStatement(expression: LighterASTNode, errorReason: String = ""): FirStatement {
        return when (expression.tokenType) {
            // Always FirExpression
            LAMBDA_EXPRESSION -> convertLambdaExpression(expression)
            BINARY_WITH_TYPE, IS_EXPRESSION -> convertBinaryWithTypeRHSExpression(expression)
            PREFIX_EXPRESSION, POSTFIX_EXPRESSION -> convertUnaryExpression(expression)
            CLASS_LITERAL_EXPRESSION -> convertClassLiteralExpression(expression)
            CALLABLE_REFERENCE_EXPRESSION -> convertCallableReferenceExpression(expression)
            in QUALIFIED_ACCESS -> convertQualifiedExpression(expression)
            CALL_EXPRESSION -> convertCallExpression(expression)
            WHEN -> convertWhenExpression(expression)
            ARRAY_ACCESS_EXPRESSION -> convertArrayAccessExpression(expression)
            COLLECTION_LITERAL_EXPRESSION -> convertCollectionLiteralExpression(expression)
            STRING_TEMPLATE -> convertStringTemplate(expression)
            is KtConstantExpressionElementType -> convertConstantExpression(expression)
            REFERENCE_EXPRESSION -> convertSimpleNameExpression(expression)
            FOR -> convertFor(expression) // FirBlock
            TRY -> convertTryExpression(expression)
            IF -> convertIfExpression(expression)
            BREAK, CONTINUE -> convertLoopJump(expression)
            RETURN -> convertReturn(expression)
            THROW -> convertThrow(expression)
            PARENTHESIZED -> {
                val content = expression.getExpressionInParentheses()
                context.forwardLabelUsagePermission(expression, content)
                getAsFirExpression(content, "Empty parentheses", sourceWhenInvalidExpression = expression)
            }
            PROPERTY_DELEGATE, INDICES, CONDITION, LOOP_RANGE ->
                getAsFirExpression(expression.getChildExpression(), errorReason, sourceWhenInvalidExpression = expression)
            THIS_EXPRESSION -> convertThisExpression(expression)
            SUPER_EXPRESSION -> convertSuperExpression(expression)
            OBJECT_LITERAL -> declarationBuilder.convertObjectLiteral(expression)
            DESTRUCTURING_DECLARATION -> declarationBuilder.convertDestructingDeclaration(expression)
                .toFirDestructingDeclaration(this, baseModuleData) // FirBlock

            // Sometimes non-expression FirStatement
            BINARY_EXPRESSION -> convertBinaryExpression(expression)
            LABELED_EXPRESSION -> convertLabeledExpression(expression)
            ANNOTATED_EXPRESSION -> convertAnnotatedExpression(expression)
            FUN -> declarationBuilder.convertFunctionDeclaration(expression)

            // Always non-expression FirStatement
            DO_WHILE -> convertDoWhile(expression)
            WHILE -> convertWhile(expression)
            PROPERTY -> declarationBuilder.convertPropertyDeclaration(expression)
            CLASS, OBJECT_DECLARATION -> declarationBuilder.convertClass(expression)
            TYPEALIAS -> declarationBuilder.convertTypeAlias(expression)

            else -> buildErrorExpression(
                expression.toFirSourceElement(KtFakeSourceElementKind.ErrorTypeRef),
                ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFunctionLiteral
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitLambdaExpression
     */
    private fun convertLambdaExpression(lambdaExpression: LighterASTNode): FirAnonymousFunctionExpression {
        val valueParameterList = mutableListOf<ValueParameter>()
        var block: LighterASTNode? = null
        var hasArrow = false

        val functionSymbol = FirAnonymousFunctionSymbol()
        lambdaExpression.getChildNodesByType(FUNCTION_LITERAL).first().forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER_LIST -> valueParameterList += declarationBuilder.convertValueParameters(
                    valueParameters = it,
                    functionSymbol,
                    ValueParameterDeclaration.LAMBDA
                )
                BLOCK -> block = it
                ARROW -> hasArrow = true
            }
        }

        val expressionSource = lambdaExpression.toFirSourceElement()
        val target: FirFunctionTarget
        val anonymousFunction = buildAnonymousFunction {
            source = expressionSource
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = implicitType
            receiverParameter = expressionSource.asReceiverParameter(moduleData, functionSymbol)
            symbol = functionSymbol
            isLambda = true
            hasExplicitParameterList = hasArrow
            label = context.getLastLabel(lambdaExpression) ?: context.calleeNamesForLambda.lastOrNull()?.let {
                buildLabel {
                    source = expressionSource.fakeElement(KtFakeSourceElementKind.GeneratedLambdaLabel)
                    name = it.asString()
                }
            }
            target = FirFunctionTarget(labelName = label?.name, isLambda = true)
            context.firFunctionTargets += target
            val destructuringStatements = mutableListOf<FirStatement>()
            for (valueParameter in valueParameterList) {
                val multiDeclaration = valueParameter.destructuringDeclaration
                valueParameters += if (multiDeclaration != null) {
                    val name = SpecialNames.DESTRUCT
                    val multiParameter = buildValueParameter {
                        source = valueParameter.firValueParameter.source
                        containingDeclarationSymbol = functionSymbol
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = valueParameter.firValueParameter.returnTypeRef
                        this.name = name
                        symbol = FirValueParameterSymbol()
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                    addDestructuringStatements(
                        destructuringStatements,
                        baseModuleData,
                        multiDeclaration,
                        multiParameter,
                        isTmpVariable = false,
                        forceLocal = true,
                    )
                    multiParameter
                } else {
                    valueParameter.firValueParameter
                }
            }

            body = withForcedLocalContext {
                if (block != null) {
                    val kind = runIf(destructuringStatements.isNotEmpty()) {
                        KtFakeSourceElementKind.LambdaDestructuringBlock
                    }
                    val bodyBlock = declarationBuilder.convertBlockExpressionWithoutBuilding(block, kind).apply {
                        if (statements.isEmpty()) {
                            statements.add(
                                buildReturnExpression {
                                    source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody)
                                    this.target = target
                                    result = buildUnitExpression {
                                        source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitUnit.ForEmptyLambda)
                                    }
                                }
                            )
                        }
                    }.build()

                    if (destructuringStatements.isNotEmpty()) {
                        // Destructured variables must be in a separate block so that they can be shadowed.
                        buildBlock {
                            source = bodyBlock.source?.realElement()
                            statements.addAll(destructuringStatements)
                            statements.add(bodyBlock)
                        }
                    } else {
                        bodyBlock
                    }
                } else {
                    buildSingleExpressionBlock(buildErrorExpression(expressionSource, ConeSyntaxDiagnostic("Lambda has no body")))
                }
            }
            context.firFunctionTargets.removeLast()
        }.also {
            target.bind(it)
        }
        return buildAnonymousFunctionExpression {
            source = expressionSource
            this.anonymousFunction = anonymousFunction
        }
    }

    /**
     * Attempts to fold a binary expression involving string concatenation into a single string concatenation call.
     *
     * This method traverses the provided binary expression, extracting all string template nodes and converting
     * the expression into a unified string concatenation call. The method handles nested expressions by pushing
     * nodes onto an input stack and processing them iteratively.
     *
     * @return A `FirExpression` representing a single string concatenation call if the folding was successful;
     * `null` if the binary expression could not be folded.
     */
    private fun tryFoldStringConcatenation(binaryExpression: LighterASTNode): FirExpression? {
        val input = mutableListOf<LighterASTNode?>()
        val output = mutableListOf<LighterASTNode?>()
        input.add(binaryExpression)
        while (input.isNotEmpty()) {
            val node = input.pop()
            when (node?.tokenType) {
                BINARY_EXPRESSION -> {
                    val (leftNode, operationReference, rightNode) = extractBinaryExpression(node)

                    if (operationReference.getOperationSymbol(tree) != PLUS) {
                        return null
                    }

                    input.add(leftNode)
                    input.add(rightNode)
                }
                else -> {
                    if (node?.tokenType != STRING_TEMPLATE) {
                        return null
                    } else {
                        output.add(node)
                    }
                }
            }
        }

        return buildStringConcatenationCall {
            val stringConcatenationSource = binaryExpression.toFirSourceElement()
            argumentList = buildArgumentList {
                arguments += output.asReversed().map {
                    getAsFirExpression<FirExpression>(it, sourceWhenInvalidExpression = binaryExpression)
                }
                source = stringConcatenationSource
            }
            source = stringConcatenationSource
            interpolationPrefix = ""
            isFoldedStrings = true
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseBinaryExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitBinaryExpression
     */
    private fun convertBinaryExpression(binaryExpression: LighterASTNode): FirStatement {
        return tryFoldStringConcatenation(binaryExpression) ?: convertBinaryExpressionFallback(binaryExpression)
    }

    private fun extractBinaryExpression(binaryExpression: LighterASTNode): Triple<LighterASTNode?, LighterASTNode, LighterASTNode?> {
        var left: LighterASTNode? = null
        var op: LighterASTNode? = null
        var right: LighterASTNode? = null
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    op = it
                }
                else -> if (it.isExpression()) {
                    if (op == null) {
                        left = it
                    } else {
                        right = it
                    }
                }
            }
        }
        return Triple(left, op!!, right)
    }

    private fun convertBinaryExpressionFallback(binaryExpression: LighterASTNode): FirStatement {
        val (leftArgNode, operationReference, rightArgNode) = extractBinaryExpression(binaryExpression)
        val operationReferenceSource = operationReference.toFirSourceElement()
        val operationTokenName = operationReference.asText
        val operationToken = operationReference.getOperationSymbol(tree)
        val baseSource = binaryExpression.toFirSourceElement()
        if (operationToken == IDENTIFIER) {
            context.calleeNamesForLambda += operationTokenName.nameAsSafeName()
        } else {
            context.calleeNamesForLambda += null
        }

        val rightArgAsFir = if (rightArgNode != null)
            getAsFirExpression<FirExpression>(rightArgNode, "No right operand")
        else
            buildErrorExpression(binaryExpression.toFirSourceElement(), ConeSyntaxDiagnostic("No right operand"))

        val leftArgAsFir = getAsFirExpression<FirExpression>(leftArgNode, "No left operand", sourceWhenInvalidExpression = binaryExpression)

        // No need for the callee name since arguments are already generated
        context.calleeNamesForLambda.removeLast()

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
                    source = operationReferenceSource
                    name = conventionCallName ?: operationTokenName.nameAsSafeName()
                }
                explicitReceiver = leftArgAsFir
                argumentList = buildUnaryArgumentList(rightArgAsFir)
                origin = if (conventionCallName != null) FirFunctionCallOrigin.Operator else FirFunctionCallOrigin.Infix
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            if (firOperation in FirOperation.ASSIGNMENTS) {
                leftArgNode.generateAssignment(
                    binaryExpression.toFirSourceElement(),
                    leftArgNode?.toFirSourceElement(),
                    rightArgAsFir,
                    firOperation,
                    leftArgAsFir.annotations,
                    rightArgNode,
                    leftArgNode?.tokenType in UNWRAPPABLE_TOKEN_TYPES,
                ) {
                    getAsFirExpression<FirExpression>(
                        this,
                        "Incorrect expression in assignment",
                        sourceWhenInvalidExpression = binaryExpression,
                        isValidExpression = { !it.isStatementLikeExpression || it.isArraySet },
                    )
                }
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
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitBinaryWithTypeRHSExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitIsExpression
     */
    private fun convertBinaryWithTypeRHSExpression(binaryExpression: LighterASTNode): FirTypeOperatorCall {
        lateinit var operationReference: LighterASTNode
        var leftArgAsFir: FirExpression? = null
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationReference = it
                TYPE_REFERENCE -> firType = declarationBuilder.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
            }
        }

        return buildTypeOperatorCall {
            source = binaryExpression.toFirSourceElement()
            operation = operationReference.getOperationSymbol(tree).toFirOperation()
            conversionTypeRef = firType
            argumentList = buildUnaryArgumentList(
                leftArgAsFir ?: buildErrorExpression(binaryExpression.toFirSourceElement(), ConeSyntaxDiagnostic("No left operand"))
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLabeledExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitLabeledExpression
     */
    private fun convertLabeledExpression(labeledExpression: LighterASTNode): FirStatement {
        var firExpression: FirStatement? = null
        var labelSource: KtSourceElement? = null
        var forbiddenLabelKind: ForbiddenLabelKind? = null

        val isRepetitiveLabel = labeledExpression.getLabeledExpression()?.tokenType == LABELED_EXPRESSION

        labeledExpression.forEachChildren {
            context.setNewLabelUserNode(it)
            when (it.tokenType) {
                LABEL_QUALIFIER -> {
                    val name = it.asText.dropLast(1)
                    labelSource = it.getChildNodesByType(LABEL).single().toFirSourceElement()
                    context.addNewLabel(buildLabel(name, labelSource))
                    forbiddenLabelKind = getForbiddenLabelKind(name, isRepetitiveLabel)
                }
                BLOCK -> firExpression = declarationBuilder.convertBlock(it)
                PROPERTY -> firExpression = declarationBuilder.convertPropertyDeclaration(it)
                else -> if (it.isExpression()) firExpression = getAsFirStatement(it)
            }
        }

        context.dropLastLabel()

        // Cast is safe because firExpression is FirStatement?
        return buildExpressionHandlingLabelErrors(
            firExpression,
            labeledExpression.toFirSourceElement(),
            forbiddenLabelKind,
            labelSource,
        ) as FirStatement
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitUnaryExpression
     */
    private fun convertUnaryExpression(unaryExpression: LighterASTNode): FirExpression {
        var argument: LighterASTNode? = null
        lateinit var operationReference: LighterASTNode
        unaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> {
                    operationReference = it
                }
                else -> if (it.isExpression()) argument = it
            }
        }

        val operationToken = operationReference.getOperationSymbol(tree)
        val conventionCallName = operationToken.toUnaryName()
        return when {
            operationToken == EXCLEXCL -> {
                buildCheckNotNullCall {
                    source = unaryExpression.toFirSourceElement()
                    argumentList = buildUnaryArgumentList(
                        getAsFirExpression<FirExpression>(
                            argument,
                            "No operand",
                            sourceWhenInvalidExpression = unaryExpression
                        )
                    )
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
                val receiver = getAsFirExpression<FirExpression>(argument, "No operand", sourceWhenInvalidExpression = unaryExpression)
                convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary(unaryExpression, receiver, operationToken)?.let { return it }
                buildFunctionCall {
                    source = unaryExpression.toFirSourceElement()
                    calleeReference = buildSimpleNamedReference {
                        source = operationReference.toFirSourceElement()
                        name = conventionCallName
                    }
                    explicitReceiver = receiver
                    origin = FirFunctionCallOrigin.Operator
                }
            }
            else -> throw IllegalStateException("Unexpected expression: ${unaryExpression.asText}")
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePrefixExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitAnnotatedExpression
     */
    private fun convertAnnotatedExpression(annotatedExpression: LighterASTNode): FirStatement {
        var firExpression: FirStatement? = null
        val firAnnotationList = mutableListOf<FirAnnotation>()
        annotatedExpression.forEachChildren {
            when (it.tokenType) {
                ANNOTATION -> declarationBuilder.convertAnnotationTo(it, firAnnotationList)
                ANNOTATION_ENTRY -> firAnnotationList += declarationBuilder.convertAnnotationEntry(it)
                BLOCK -> firExpression = declarationBuilder.convertBlockExpression(it)
                else -> if (it.isExpression()) {
                    context.forwardLabelUsagePermission(annotatedExpression, it)
                    firExpression = getAsFirStatement(it)
                }
            }
        }

        val result = firExpression ?: buildErrorExpression(annotatedExpression.toFirSourceElement(), ConeNotAnnotationContainer("???"))
        result.replaceAnnotations(result.annotations.smartPlus(firAnnotationList))
        return result
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitClassLiteralExpression
     */
    private fun convertClassLiteralExpression(classLiteralExpression: LighterASTNode): FirGetClassCall {
        var firReceiverExpression: FirExpression? = null
        classLiteralExpression.forEachChildren {
            if (it.isExpression()) firReceiverExpression = getAsFirExpression(it, "No receiver in class literal")
        }

        val classLiteralSource = classLiteralExpression.toFirSourceElement()

        return buildGetClassCall {
            source = classLiteralSource
            argumentList = buildUnaryArgumentList(
                firReceiverExpression
                    ?: buildErrorExpression(classLiteralSource, ConeUnsupportedClassLiteralsWithEmptyLhs)
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitCallableReferenceExpression
     */
    private fun convertCallableReferenceExpression(callableReferenceExpression: LighterASTNode): FirCallableReferenceAccess {
        var isReceiver = true
        var hasQuestionMarkAtLHS = false
        var firReceiverExpression: FirExpression? = null
        lateinit var namedReference: FirNamedReference
        callableReferenceExpression.forEachChildren {
            when (it.tokenType) {
                COLONCOLON -> isReceiver = false
                QUEST -> hasQuestionMarkAtLHS = true
                else -> if (it.isExpression()) {
                    if (isReceiver) {
                        firReceiverExpression = getAsFirExpression(it, "Incorrect receiver expression")
                    } else {
                        namedReference = createSimpleNamedReference(it.toFirSourceElement(), it)
                    }
                }
            }
        }

        return buildCallableReferenceAccess {
            source = callableReferenceExpression.toFirSourceElement()
            calleeReference = namedReference
            explicitReceiver = firReceiverExpression
            this.hasQuestionMarkAtLHS = hasQuestionMarkAtLHS
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitQualifiedExpression
     */
    private fun convertQualifiedExpression(dotQualifiedExpression: LighterASTNode): FirExpression {
        var isSelector = false
        var isSafe = false
        var firSelector: FirExpression? = null
        var firReceiver: FirExpression? = null //before dot
        dotQualifiedExpression.forEachChildren {
            when (val tokenType = it.tokenType) {
                DOT -> isSelector = true
                SAFE_ACCESS -> {
                    isSafe = true
                    isSelector = true
                }
                else -> {
                    val isEffectiveSelector = isSelector && tokenType != TokenType.ERROR_ELEMENT
                    val firExpression =
                        getAsFirExpression<FirExpression>(it, "Incorrect ${if (isEffectiveSelector) "selector" else "receiver"} expression")
                    if (isEffectiveSelector) {
                        val callExpressionCallee = if (tokenType == CALL_EXPRESSION) it.getFirstChildExpressionUnwrapped() else null
                        firSelector =
                            if (tokenType is KtNameReferenceExpressionElementType ||
                                (tokenType == CALL_EXPRESSION && callExpressionCallee?.tokenType != LAMBDA_EXPRESSION)
                            ) {
                                firExpression
                            } else {
                                buildErrorExpression {
                                    source = callExpressionCallee?.toFirSourceElement() ?: it.toFirSourceElement()
                                    diagnostic = ConeSimpleDiagnostic(
                                        "The expression cannot be a selector (occur after a dot)",
                                        if (callExpressionCallee == null) DiagnosticKind.IllegalSelector else DiagnosticKind.NoReceiverAllowed
                                    )
                                    expression = firExpression
                                }
                            }
                    } else {
                        firReceiver = firExpression
                    }
                }
            }
        }

        return when (firSelector) {
            is FirQualifiedAccessExpression -> {
                if (isSafe) {
                    @OptIn(FirImplementationDetail::class)
                    firSelector.replaceSource(dotQualifiedExpression.toFirSourceElement(KtFakeSourceElementKind.DesugaredSafeCallExpression))
                    return firSelector.createSafeCall(
                        firReceiver!!,
                        dotQualifiedExpression.toFirSourceElement()
                    )
                }
                convertFirSelector(firSelector, dotQualifiedExpression.toFirSourceElement(), firReceiver!!)
            }
            is FirErrorExpression if firReceiver != null -> {
                buildQualifiedErrorAccessExpression {
                    this.receiver = firReceiver
                    this.selector = firSelector
                    source = dotQualifiedExpression.toFirSourceElement()
                    diagnostic = ConeSyntaxDiagnostic("Qualified expression with unexpected selector")
                }
            }
            else -> {
                buildErrorExpression {
                    source = dotQualifiedExpression.toFirSourceElement()
                    diagnostic = ConeSyntaxDiagnostic("Qualified expression without selector")

                    // if there is no selector, we still want to resolve the receiver
                    expression = firReceiver
                }
            }
        }
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
                    PARENTHESIZED -> if (node.tokenType != TokenType.ERROR_ELEMENT) {
                        additionalArgument = getAsFirExpression(
                            node.getExpressionInParentheses(),
                            "Incorrect invoke receiver",
                            sourceWhenInvalidExpression = node
                        )
                    }
                    TYPE_ARGUMENT_LIST -> {
                        firTypeArguments += declarationBuilder.convertTypeArguments(node, allowedUnderscoredTypeArgument = true)
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

        // TODO(KT-22765) drop workaround when suspend modifier for lambdas is implemented
        if (imitateLambdaSuspendModifier &&
            name == StandardClassIds.Callables.suspend.callableName.identifier &&
            !callSuffix.getParent().let { it.selectorExpression == callSuffix && it.receiverExpression != null } &&
            valueArguments.singleOrNull()?.tokenType == LAMBDA_ARGUMENT &&
            firTypeArguments.isEmpty()
        ) {
            valueArguments.single().getFirstChild()?.let {
                return getAsFirExpression<FirAnonymousFunctionExpression>(it).apply {
                    anonymousFunction.replaceStatus(anonymousFunction.status.copy(isSuspend = true))
                }
            }
        }

        val (calleeReference, receiverForInvoke) = when {
            name != null -> CalleeAndReceiver(
                buildSimpleNamedReference {
                    this.source = callSuffix.getFirstChildExpressionUnwrapped()?.toFirSourceElement() ?: source
                    this.name = name.nameAsSafeName()
                }
            )

            superNode != null || additionalArgument is FirSuperReceiverExpression -> {
                CalleeAndReceiver(
                    buildErrorNamedReferenceWithNoName(
                        source = superNode?.toFirSourceElement() ?: (additionalArgument as? FirResolvable)?.calleeReference?.source,
                        diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed),
                    )
                )
            }

            additionalArgument != null -> {
                CalleeAndReceiver(
                    buildSimpleNamedReference {
                        this.source = source
                        this.name = OperatorNameConventions.INVOKE
                    },
                    additionalArgument,
                )
            }

            else -> CalleeAndReceiver(
                buildErrorNamedReferenceWithNoName(
                    diagnostic = ConeSyntaxDiagnostic("Call has no callee"),
                    source,
                )
            )
        }

        val builder: FirQualifiedAccessExpressionBuilder = if (hasArguments) {
            val builder = if (receiverForInvoke != null) FirImplicitInvokeCallBuilder() else FirFunctionCallBuilder()
            builder.apply {
                this.source = source
                this.calleeReference = calleeReference

                context.calleeNamesForLambda += calleeReference.name
                this.extractArgumentsFrom(valueArguments.flatMap { convertValueArguments(it) })
                context.calleeNamesForLambda.removeLast()
            }
        } else {
            FirPropertyAccessExpressionBuilder().apply {
                this.source = source
                this.calleeReference = calleeReference
            }
        }
        return builder.apply {
            this.explicitReceiver = receiverForInvoke
            typeArguments += firTypeArguments
        }.build().pullUpSafeCallIfNecessary()
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseStringTemplate
     */
    private fun convertStringTemplate(stringTemplate: LighterASTNode): FirExpression {
        val children = stringTemplate.getChildrenAsArray()
        return children.toInterpolatingCall(
            stringTemplate,
            convertTemplateEntry = { convertShortOrLongStringTemplate(it) },
            prefix = { children.firstOrNull { it?.tokenType == STRING_INTERPOLATION_PREFIX }?.asText ?: "" }
        )
    }

    private fun LighterASTNode?.convertShortOrLongStringTemplate(errorReason: String): Collection<FirExpression> {
        val firExpressions = mutableListOf<FirExpression>()
        this?.forEachChildren {
            when (it.tokenType) {
                LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END, SHORT_TEMPLATE_ENTRY_START -> return@forEachChildren
                else -> firExpressions.add(getAsFirExpression(it, errorReason))
            }
        }
        return firExpressions
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLiteralConstant
     */
    private fun convertConstantExpression(constantExpression: LighterASTNode): FirExpression {
        return generateConstantExpressionByLiteral(constantExpression)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhen
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitWhenExpression
     */
    private fun convertWhenExpression(whenExpression: LighterASTNode): FirWhenExpression {
        var subjectExpression: FirExpression? = null
        var subjectVariable: FirVariable? = null
        val whenEntryNodes = mutableListOf<LighterASTNode>()
        val whenEntries = mutableListOf<WhenEntry>()
        whenExpression.forEachChildren {
            when (it.tokenType) {
                PROPERTY -> subjectVariable = (declarationBuilder.convertPropertyDeclaration(it) as FirVariable).let { variable ->
                    buildProperty {
                        source = it.toFirSourceElement()
                        origin = FirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        returnTypeRef = variable.returnTypeRef
                        name = variable.name
                        initializer = variable.initializer
                        isVar = false
                        symbol = FirLocalPropertySymbol()
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                        isLocal = true
                        receiverParameter = variable.receiverParameter?.let { receiverParameter ->
                            buildReceiverParameterCopy(receiverParameter) {
                                symbol = FirReceiverParameterSymbol()
                                containingDeclarationSymbol = this@buildProperty.symbol
                            }
                        }
                        annotations += variable.annotations
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

        if (hasSubject && subjectVariable == null) {
            val name = SpecialNames.WHEN_SUBJECT
            subjectVariable = buildProperty {
                source = subjectExpression.source?.fakeElement(KtFakeSourceElementKind.WhenGeneratedSubject)
                origin = FirDeclarationOrigin.Synthetic.ImplicitWhenSubject
                moduleData = baseModuleData
                returnTypeRef = FirImplicitTypeRefImplWithoutSource
                this.name = name
                initializer = subjectExpression
                isVar = false
                symbol = FirLocalPropertySymbol()
                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                isLocal = true
            }
        }

        @OptIn(FirContractViolation::class)
        val subject = FirExpressionRef<FirWhenExpression>()
        var shouldBind = hasSubject
        whenEntryNodes.mapTo(whenEntries) {
            convertWhenEntry(it, subjectVariable)
        }
        return buildWhenExpression {
            source = whenExpression.toFirSourceElement()
            this.subjectVariable = subjectVariable
            usedAsExpression = whenExpression.usedAsExpression
            for (entry in whenEntries) {
                shouldBind = shouldBind || entry.shouldBindSubject
                val branch = entry.firBlock
                val entrySource = entry.node.toFirSourceElement()
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        val firCondition = entry.toFirWhenCondition()
                        buildWhenBranch(hasGuard = entry.guard != null) {
                            source = entrySource
                            condition = firCondition.guardedBy(entry.guard)
                            result = branch
                        }
                    } else {
                        val firCondition = entry.toFirWhenConditionWithoutSubject()
                        buildWhenBranch(hasGuard = entry.guard != null) {
                            source = entrySource
                            condition = firCondition.guardedBy(entry.guard)
                            result = branch
                        }
                    }
                } else {
                    buildWhenBranch(hasGuard = entry.guard != null) {
                        source = entrySource
                        condition = entry.guard ?: buildElseIfTrueCondition()
                        result = branch
                    }
                }
            }
        }.also {
            if (shouldBind) {
                subject.bind(it)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntry
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhenEntryNotElse
     */
    private fun convertWhenEntry(
        whenEntry: LighterASTNode,
        subjectVariable: FirVariable?,
    ): WhenEntry {
        var isElse = false
        var firBlock: FirBlock = buildEmptyExpressionBlock()
        val conditions = mutableListOf<FirExpression>()
        var guard: FirExpression? = null
        var shouldBindSubject = false
        whenEntry.forEachChildren {
            when (it.tokenType) {
                WHEN_CONDITION_EXPRESSION -> conditions += convertWhenConditionExpression(it, subjectVariable)
                WHEN_CONDITION_IN_RANGE -> {
                    val (condition, shouldBind) = convertWhenConditionInRange(it, subjectVariable)
                    conditions += condition
                    shouldBindSubject = shouldBindSubject || shouldBind
                }
                WHEN_CONDITION_IS_PATTERN -> {
                    val (condition, shouldBind) = convertWhenConditionIsPattern(it, subjectVariable)
                    conditions += condition
                    shouldBindSubject = shouldBindSubject || shouldBind
                }
                WHEN_ENTRY_GUARD -> guard = getAsFirExpression(
                    it.getFirstChildExpressionUnwrapped(),
                    "No expression in guard",
                    sourceWhenInvalidExpression = it
                )
                ELSE_KEYWORD -> isElse = true
                BLOCK -> firBlock = declarationBuilder.convertBlock(it)
                else -> if (it.isExpression()) firBlock = declarationBuilder.convertBlock(it)
            }
        }

        return WhenEntry(conditions, guard, firBlock, whenEntry, isElse, shouldBindSubject, tree)
    }

    private fun convertWhenConditionExpression(
        whenCondition: LighterASTNode,
        subjectVariable: FirVariable?,
    ): FirExpression {
        var firExpression: FirExpression? = null
        whenCondition.forEachChildren {
            when (it.tokenType) {
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No expression in condition with expression")
            }
        }

        val calculatedFirExpression = firExpression ?: buildErrorExpression(
            source = whenCondition.toFirSourceElement(),
            ConeSyntaxDiagnostic("No expression in condition with expression")
        )

        if (subjectVariable == null) {
            return calculatedFirExpression
        }

        val conditionSource = whenCondition.toFirSourceElement(KtFakeSourceElementKind.WhenCondition)

        return buildEqualityOperatorCall {
            source = conditionSource
            operation = FirOperation.EQ
            argumentList = buildBinaryArgumentList(
                left = buildWhenSubjectAccess(whenCondition.toFirSourceElement(), subjectVariable),
                right = calculatedFirExpression
            )
        }
    }

    private data class WhenConditionConvertedResults(val expression: FirExpression, val shouldBindSubject: Boolean)

    private fun convertWhenConditionInRange(
        whenCondition: LighterASTNode,
        subjectVariable: FirVariable?,
    ): WhenConditionConvertedResults {
        var isNegate = false
        var firExpression: FirExpression? = null
        var conditionSource: KtLightSourceElement? = null
        whenCondition.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE if it.asText == NOT_IN.value -> {
                    conditionSource = it.toFirSourceElement()
                    isNegate = true
                }
                OPERATION_REFERENCE -> {
                    conditionSource = it.toFirSourceElement()
                }
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No range in condition with range")
            }
        }

        val subjectExpression = buildWhenSubjectAccess(whenCondition.toFirSourceElement(), subjectVariable)

        val calculatedFirExpression = firExpression ?: buildErrorExpression(
            whenCondition.toFirSourceElement(),
            ConeSyntaxDiagnostic("No range in condition with range")
        )

        val result = calculatedFirExpression.generateContainsOperation(
            subjectExpression,
            inverted = isNegate,
            baseSource = whenCondition.toFirSourceElement(),
            operationReferenceSource = conditionSource
        )
        return createWhenConditionConvertedResults(subjectVariable != null, result, whenCondition)
    }

    private fun convertWhenConditionIsPattern(
        whenCondition: LighterASTNode,
        subjectVariable: FirVariable?,
    ): WhenConditionConvertedResults {
        lateinit var firOperation: FirOperation
        var firType: FirTypeRef? = null
        whenCondition.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> firType = declarationBuilder.convertType(it)
                IS_KEYWORD -> firOperation = FirOperation.IS
                NOT_IS -> firOperation = FirOperation.NOT_IS
            }
        }

        val subjectExpression = buildWhenSubjectAccess(whenCondition.toFirSourceElement(), subjectVariable)

        val result = buildTypeOperatorCall {
            source = whenCondition.toFirSourceElement()
            operation = firOperation
            conversionTypeRef = firType ?: buildErrorTypeRef {
                diagnostic = ConeSyntaxDiagnostic("Incomplete code")
                source = whenCondition.toFirSourceElement()
            }
            argumentList = buildUnaryArgumentList(subjectExpression)
        }

        return createWhenConditionConvertedResults(subjectVariable != null, result, whenCondition)
    }

    private fun createWhenConditionConvertedResults(
        hasSubject: Boolean,
        result: FirExpression,
        whenCondition: LighterASTNode,
    ): WhenConditionConvertedResults {
        return if (hasSubject) {
            WhenConditionConvertedResults(result, false)
        } else {
            WhenConditionConvertedResults(
                buildErrorExpression {
                    source = whenCondition.toFirSourceElement()
                    diagnostic = ConeSimpleDiagnostic(
                        "No expression in condition with expression",
                        DiagnosticKind.ExpressionExpected
                    )
                    nonExpressionElement = result
                },
                true,
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseArrayAccess
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitArrayAccessExpression
     */
    private fun convertArrayAccessExpression(arrayAccess: LighterASTNode): FirExpression {
        var firExpression: FirExpression? = null
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
                source = arrayAccess.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ArrayAccessNameReference)
                name = if (isGet) OperatorNameConventions.GET else OperatorNameConventions.SET
            }
            explicitReceiver =
                firExpression ?: buildErrorExpression(arrayAccess.toFirSourceElement(), ConeSyntaxDiagnostic("No array expression"))
            argumentList = buildArgumentList {
                arguments += indices
                getArgument?.let { arguments += it }
            }
            origin = FirFunctionCallOrigin.Operator
        }.pullUpSafeCallIfNecessary()
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseCollectionLiteralExpression
     */
    private fun convertCollectionLiteralExpression(expression: LighterASTNode): FirCollectionLiteral {
        val firExpressionList = mutableListOf<FirExpression>()
        expression.forEachChildren {
            if (it.isExpression()) firExpressionList += getAsFirExpression<FirExpression>(it, "Incorrect collection literal argument")
        }
        val arguments = buildArgumentList {
            arguments += firExpressionList
        }
        return buildCollectionLiteral {
            source = expression.toFirSourceElement()
            argumentList = arguments
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
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitSimpleNameExpression
     */
    private fun convertSimpleNameExpression(referenceExpression: LighterASTNode): FirQualifiedAccessExpression {
        val nameSource = referenceExpression.toFirSourceElement()
        val referenceSourceElement = if (nameSource.kind is KtFakeSourceElementKind) {
            nameSource
        } else {
            nameSource.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
        }

        return buildPropertyAccessExpression {
            source = nameSource
            calleeReference = createSimpleNamedReference(referenceSourceElement, referenceExpression)
        }
    }

    private fun createSimpleNamedReference(
        sourceElement: KtSourceElement,
        referenceExpression: LighterASTNode,
    ): FirNamedReference {
        return buildSimpleNamedReference {
            source = sourceElement
            name = referenceExpression.asText.nameAsSafeName()
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoWhile
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitDoWhileExpression
     */
    private fun convertDoWhile(doWhileLoop: LighterASTNode): FirLoop {
        var block: LighterASTNode? = null
        var firCondition: FirExpression? = null

        val target: FirLoopTarget
        return FirDoWhileLoopBuilder().apply {
            source = doWhileLoop.toFirSourceElement()
            // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
            target = prepareTarget(doWhileLoop)
            doWhileLoop.forEachChildren {
                when (it.tokenType) {
                    BODY -> block = it
                    CONDITION -> firCondition = getAsFirExpression(it, "No condition in do-while loop")
                }
            }
            condition = firCondition ?: buildErrorExpression(
                doWhileLoop.toFirSourceElement(),
                ConeSyntaxDiagnostic("No condition in do-while loop")
            )
        }.configure(target) { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhile
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitWhileExpression
     */
    private fun convertWhile(whileLoop: LighterASTNode): FirLoop {
        var block: LighterASTNode? = null
        var firCondition: FirExpression? = null
        whileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in while loop")
            }
        }

        val target: FirLoopTarget
        return FirWhileLoopBuilder().apply {
            source = whileLoop.toFirSourceElement()
            condition =
                firCondition ?: buildErrorExpression(whileLoop.toFirSourceElement(), ConeSyntaxDiagnostic("No condition in while loop"))
            // break/continue in the while loop condition will refer to an outer loop if any.
            // So, prepare the loop target after building the condition.
            target = prepareTarget(whileLoop)
        }.configure(target) { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFor
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitForExpression
     */
    private fun convertFor(forLoop: LighterASTNode): FirBlock {
        var parameter: ValueParameter? = null
        var rangeExpression: FirExpression? = null
        var blockNode: LighterASTNode? = null
        forLoop.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> parameter = declarationBuilder.convertValueParameter(it, null, ValueParameterDeclaration.FOR_LOOP)
                LOOP_RANGE -> rangeExpression = getAsFirExpression(it, "No range in for loop")
                BODY -> blockNode = it
            }
        }

        val calculatedRangeExpression =
            rangeExpression ?: buildErrorExpression(forLoop.toFirSourceElement(), ConeSyntaxDiagnostic("No range in for loop"))
        val fakeSource = forLoop.toFirSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
        val rangeSource = calculatedRangeExpression.source?.fakeElement(KtFakeSourceElementKind.DesugaredForLoop) ?: fakeSource
        val target: FirLoopTarget
        // NB: FirForLoopChecker relies on this block existence and structure
        return buildBlock {
            source = fakeSource
            val iteratorVal = generateTemporaryVariable(
                baseModuleData,
                rangeSource,
                SpecialNames.ITERATOR,
                buildFunctionCall {
                    source = rangeSource
                    calleeReference = buildSimpleNamedReference {
                        source = rangeSource
                        name = OperatorNameConventions.ITERATOR
                    }
                    explicitReceiver = calculatedRangeExpression
                    origin = FirFunctionCallOrigin.Operator
                }
            )
            statements += iteratorVal
            statements += FirWhileLoopBuilder().apply {
                source = fakeSource
                condition = buildFunctionCall {
                    source = rangeSource
                    calleeReference = buildSimpleNamedReference {
                        source = rangeSource
                        name = OperatorNameConventions.HAS_NEXT
                    }
                    explicitReceiver = generateResolvedAccessExpression(rangeSource, iteratorVal)
                    origin = FirFunctionCallOrigin.Operator
                }
                // break/continue in the for loop condition will refer to an outer loop if any.
                // So, prepare the loop target after building the condition.
                target = prepareTarget(forLoop)
            }.configure(target) {
                buildBlock block@{
                    source = blockNode?.toFirSourceElement()
                    val valueParameter = parameter ?: return@block
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    val quotedName = valueParameter.source.lighterASTNode.getChildNodeByType(IDENTIFIER)?.asText
                    val firLoopParameter = generateTemporaryVariable(
                        baseModuleData,
                        valueParameter.source,
                        name = when {
                            multiDeclaration != null -> SpecialNames.DESTRUCT
                            quotedName == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                            else -> valueParameter.name
                        },
                        buildFunctionCall {
                            source = rangeSource
                            calleeReference = buildSimpleNamedReference {
                                source = rangeSource
                                name = OperatorNameConventions.NEXT
                            }
                            explicitReceiver = generateResolvedAccessExpression(rangeSource, iteratorVal)
                            origin = FirFunctionCallOrigin.Operator
                        },
                        valueParameter.returnTypeRef,
                        extractedAnnotations = valueParameter.annotations
                    ).apply {
                        isForLoopParameter = true
                    }
                    if (multiDeclaration != null) {
                        addDestructuringStatements(
                            statements,
                            baseModuleData,
                            multiDeclaration,
                            firLoopParameter,
                            isTmpVariable = true,
                            forceLocal = true,
                        )
                    } else {
                        statements.add(firLoopParameter)
                    }
                    statements += convertLoopBody(blockNode)
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLoopBody
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.toFirBlock
     */
    private fun convertLoopBody(body: LighterASTNode?): FirBlock {
        return convertLoopOrIfBody(body) ?: buildEmptyExpressionBlock()
    }

    private fun convertLoopOrIfBody(body: LighterASTNode?): FirBlock? {
        var firBlock: FirBlock? = null
        var firStatement: FirStatement? = null
        body?.forEachChildren {
            when (it.tokenType) {
                BLOCK -> firBlock = declarationBuilder.convertBlockExpression(it)
                ANNOTATED_EXPRESSION -> {
                    if (it.getChildNodeByType(BLOCK) != null) {
                        firBlock = getAsFirExpression(it)
                    } else {
                        firStatement = getAsFirStatement(it)
                    }
                }
                else -> if (it.isExpression()) firStatement = getAsFirStatement(it)
            }
        }

        return firStatement?.let { FirSingleExpressionBlock(it) } ?: firBlock
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitTryExpression
     */
    private fun convertTryExpression(tryExpression: LighterASTNode): FirTryExpression {
        lateinit var tryBlock: FirBlock
        val catchClauses = mutableListOf<Triple<ValueParameter?, FirBlock, KtLightSourceElement>>()
        var finallyBlock: FirBlock? = null
        tryExpression.forEachChildren {
            when (it.tokenType) {
                BLOCK -> tryBlock = declarationBuilder.convertBlock(it)
                CATCH -> convertCatchClause(it)?.also { oneClause -> catchClauses += oneClause }
                FINALLY -> finallyBlock = convertFinally(it)
            }
        }
        return buildTryExpression {
            source = tryExpression.toFirSourceElement()
            this.tryBlock = tryBlock
            this.finallyBlock = finallyBlock
            for ((parameter, block, clauseSource) in catchClauses) {
                if (parameter == null) continue
                catches += buildCatch {
                    this.parameter = buildProperty {
                        source = parameter.source
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = parameter.returnTypeRef
                        isVar = false
                        status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                        isLocal = true
                        this.name = parameter.name
                        symbol = FirLocalPropertySymbol()
                        annotations += parameter.annotations
                    }.also {
                        it.isCatchParameter = true
                    }
                    this.block = block
                    this.source = clauseSource
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     */
    private fun convertCatchClause(catchClause: LighterASTNode): Triple<ValueParameter?, FirBlock, KtLightSourceElement>? {
        var valueParameter: ValueParameter? = null
        var blockNode: LighterASTNode? = null
        catchClause.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER_LIST -> valueParameter = declarationBuilder.convertValueParameters(
                    valueParameters = it,
                    FirAnonymousFunctionSymbol(),
                    ValueParameterDeclaration.CATCH
                ).firstOrNull()
                    ?: return null

                BLOCK -> blockNode = it
            }
        }

        return Triple(valueParameter, declarationBuilder.convertBlock(blockNode), catchClause.toFirSourceElement())
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

        return declarationBuilder.convertBlock(blockNode)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseIf
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitIfExpression
     */
    private fun convertIfExpression(ifExpression: LighterASTNode): FirWhenExpression {
        return buildWhenExpression {
            source = ifExpression.toFirSourceElement()
            with(parseIfExpression(ifExpression)) {
                val trueBranch = convertLoopBody(thenBlock)
                branches += buildRegularWhenBranch {
                    source = firCondition?.source
                    condition = firCondition ?: buildErrorExpression(
                        ifExpression.toFirSourceElement(),
                        ConeSyntaxDiagnostic("If statement should have condition")
                    )
                    result = trueBranch
                }

                if (elseBlock != null) {
                    val elseBranch = convertLoopOrIfBody(elseBlock)
                    if (elseBranch != null) {
                        branches += buildRegularWhenBranch {
                            source = elseBlock.toFirSourceElement()
                            condition = buildElseIfTrueCondition()
                            result = elseBranch
                        }
                    }
                }
            }
            usedAsExpression = ifExpression.usedAsExpression
        }
    }

    private class IfNodeComponents(val firCondition: FirExpression?, val thenBlock: LighterASTNode?, val elseBlock: LighterASTNode?)

    private fun parseIfExpression(ifExpression: LighterASTNode): IfNodeComponents {
        var firCondition: FirExpression? = null
        var thenBlock: LighterASTNode? = null
        var elseBlock: LighterASTNode? = null
        ifExpression.forEachChildren {
            when (it.tokenType) {
                CONDITION -> firCondition = getAsFirExpression(it, "If statement should have condition")
                THEN -> thenBlock = it
                ELSE -> elseBlock = it
            }
        }
        return IfNodeComponents(firCondition, thenBlock, elseBlock)
    }

    private val LighterASTNode.usedAsExpression: Boolean
        get() {
            var parent = getParent() ?: return true
            while (parent.elementType == ANNOTATED_EXPRESSION ||
                parent.elementType == LABELED_EXPRESSION
            ) {
                parent = parent.getParent() ?: return true
            }
            val parentTokenType = parent.tokenType
            return when (parentTokenType) {
                BLOCK -> parent.getLastChildExpression() == this && parent.usedAsExpression
                TRY, CATCH -> parent.usedAsExpression
                THEN, ELSE, WHEN_ENTRY -> parent.getParent()?.usedAsExpression ?: true
                CLASS_INITIALIZER, SCRIPT_INITIALIZER, SECONDARY_CONSTRUCTOR, FUNCTION_LITERAL, FINALLY -> false
                FUN, PROPERTY_ACCESSOR -> parent.getChildrenAsArray().any { it?.tokenType == EQ }
                DOT_QUALIFIED_EXPRESSION -> parent.getFirstChild() == this
                BODY -> when (parent.getParent()?.tokenType) {
                    FOR, WHILE, DO_WHILE -> false
                    else -> true
                }
                else -> true
            }
        }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseJump
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitBreakExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitContinueExpression
     */
    private fun convertLoopJump(jump: LighterASTNode): FirLoopJump {
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
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitReturnExpression
     */
    private fun convertReturn(returnExpression: LighterASTNode): FirReturnExpression {
        var labelName: String? = null
        var firExpression: FirExpression? = null
        returnExpression.forEachChildren {
            when (it.tokenType) {
                LABEL_QUALIFIER -> labelName = it.getAsStringWithoutBacktick().replace("@", "")
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "Incorrect return expression")
            }
        }

        val calculatedFirExpression = firExpression ?: buildUnitExpression {
            source = returnExpression.toFirSourceElement(KtFakeSourceElementKind.ImplicitUnit.Return)
        }
        return calculatedFirExpression.toReturn(
            baseSource = returnExpression.toFirSourceElement(),
            labelName = labelName,
            fromKtReturnExpression = true
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThrow
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitThrowExpression
     */
    private fun convertThrow(throwExpression: LighterASTNode): FirThrowExpression {
        var firExpression: FirExpression? = null
        throwExpression.forEachChildren {
            if (it.isExpression()) firExpression = getAsFirExpression(it, "Nothing to throw")
        }

        return buildThrowExpression {
            source = throwExpression.toFirSourceElement()
            exception = firExpression ?: buildErrorExpression(
                throwExpression.toFirSourceElement(),
                ConeSyntaxDiagnostic("Nothing to throw")
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThisExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitThisExpression
     */
    private fun convertThisExpression(thisExpression: LighterASTNode): FirThisReceiverExpression {
        val label: String? = thisExpression.getLabelName()
        return buildThisReceiverExpression {
            val sourceElement = thisExpression.toFirSourceElement()
            source = sourceElement
            calleeReference = buildExplicitThisReference {
                labelName = label
                source = sourceElement.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseSuperExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitSuperExpression
     */
    private fun convertSuperExpression(superExpression: LighterASTNode): FirSuperReceiverExpression {
        val label: String? = superExpression.getLabelName()
        var superTypeRef: FirTypeRef = implicitType
        superExpression.forEachChildren {
            when (it.tokenType) {
                TYPE_REFERENCE -> superTypeRef = declarationBuilder.convertType(it)
            }
        }

        return buildSuperReceiverExpression {
            val sourceElement = superExpression.toFirSourceElement()
            source = sourceElement
            calleeReference = buildExplicitSuperReference {
                labelName = label
                this.superTypeRef = superTypeRef
                source = sourceElement.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseValueArgumentList
     */
    fun convertValueArguments(valueArguments: LighterASTNode): List<FirExpression> {
        return valueArguments.forEachChildrenReturnList { node, container ->
            @Suppress("IncorrectFormatting")
            when (node.tokenType) {
                VALUE_ARGUMENT -> container += convertValueArgument(node)
                LAMBDA_EXPRESSION,
                LABELED_EXPRESSION,
                ANNOTATED_EXPRESSION -> container += getAsFirExpression<FirAnonymousFunctionExpression>(node).apply {
                    // TODO(KT-66553) remove and set in builder
                    @OptIn(RawFirApi::class)
                    replaceIsTrailingLambda(newIsTrailingLambda = true)
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseValueArgument
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.toFirExpression(org.jetbrains.kotlin.psi.ValueArgument)
     */
    private fun convertValueArgument(valueArgument: LighterASTNode): FirExpression {
        var identifier: String? = null
        var isSpread = false
        var firExpression: FirExpression? = null
        valueArgument.forEachChildren {
            when (it.tokenType) {
                VALUE_ARGUMENT_NAME -> identifier = it.asText
                MUL -> isSpread = true
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "Argument is absent")
            }
        }
        val calculatedFirExpression =
            firExpression ?: buildErrorExpression(valueArgument.toFirSourceElement(), ConeSyntaxDiagnostic("Argument is absent"))
        return when {
            identifier != null -> buildNamedArgumentExpression {
                source = valueArgument.toFirSourceElement()
                expression = calculatedFirExpression
                this.isSpread = isSpread
                name = identifier.nameAsSafeName()
            }
            isSpread -> buildSpreadArgumentExpression {
                source = valueArgument.toFirSourceElement()
                expression = calculatedFirExpression
            }
            else -> calculatedFirExpression
        }
    }

    override fun convertScript(
        script: LighterASTNode,
        scriptSource: KtSourceElement,
        fileName: String,
        setup: FirScriptBuilder.() -> Unit,
    ): FirScript {
        shouldNotBeCalled()
    }

    override fun convertReplSnippet(
        script: LighterASTNode,
        scriptSource: KtSourceElement,
        fileName: String,
        snippetSetup: FirReplSnippetBuilder.() -> Unit,
        statementsSetup: MutableList<FirElement>.() -> Unit,
    ): FirReplSnippet {
        shouldNotBeCalled()
    }
}
