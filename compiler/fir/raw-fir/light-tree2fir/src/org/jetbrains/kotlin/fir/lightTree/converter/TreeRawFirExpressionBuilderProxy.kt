/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.google.common.collect.ImmutableBiMap
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.NodeTypeAnalyzer
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.ForbiddenLabelKind
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameterCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
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
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class TreeRawFirExpressionBuilderProxy<Node : Any, Type : Any>(
    val analyzer: NodeTypeAnalyzer<Node, Type>,
    val context: Context<Node>,
    val baseModuleData: FirModuleData,
    val headerMode: Boolean,
) : NodeTypeAnalyzer<Node, Type> by analyzer {
    private val imitateLambdaSuspendModifier: Boolean =
        baseModuleData.session.languageVersionSettings.supportsFeature(LanguageFeature.ParseLambdaWithSuspendModifier)

    lateinit var declarationBuilder: TreeDeclarationConverter<Node>

    internal inline fun <reified R : FirExpression> getAsFirExpression(
        expression: Node,
        errorReason: String = "",
        isValidExpression: (R) -> Boolean = { !it.isStatementLikeExpression },
    ): R {
        return getAsFirExpression(expression, errorReason, expression, isValidExpression)
    }

    internal inline fun <reified R : FirExpression> getAsFirExpression(
        expression: Node?,
        errorReason: String = "",
        sourceWhenInvalidExpression: Node,
        isValidExpression: (R) -> Boolean = { !it.isStatementLikeExpression },
    ): R {
        val converted = expression?.let { getAsFirStatement(it, errorReason) }

        return wrapExpressionIfNeeded(expression, converted, isValidExpression, sourceWhenInvalidExpression, errorReason)
    }

    private inline fun <reified R : FirExpression> wrapExpressionIfNeeded(
        expression: Node?,
        converted: FirElement?,
        isValidExpression: (R) -> Boolean,
        sourceWhenInvalidExpression: Node,
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
    fun getAsFirStatement(node: Node, errorReason: String = ""): FirStatement {
        return when (node.toTokenId()) {
            // Always FirExpression
            KtNodeTypes.LAMBDA_EXPRESSION_ID -> convertLambdaExpression(node)
            KtNodeTypes.BINARY_WITH_TYPE_ID, KtNodeTypes.IS_EXPRESSION_ID -> convertBinaryWithTypeRHSExpression(node)
            KtNodeTypes.PREFIX_EXPRESSION_ID, KtNodeTypes.POSTFIX_EXPRESSION_ID -> convertUnaryExpression(node)
            KtNodeTypes.CLASS_LITERAL_EXPRESSION_ID -> convertClassLiteralExpression(node)
            KtNodeTypes.CALLABLE_REFERENCE_EXPRESSION_ID -> convertCallableReferenceExpression(node)
            in qualifiedAccessesId -> convertQualifiedExpression(node)
            KtNodeTypes.CALL_EXPRESSION_ID -> convertCallExpression(node)
            KtNodeTypes.WHEN_ID -> convertWhenExpression(node)
            KtNodeTypes.ARRAY_ACCESS_EXPRESSION_ID -> convertArrayAccessExpression(node)
            KtNodeTypes.COLLECTION_LITERAL_EXPRESSION_ID -> convertCollectionLiteralExpression(node)
            KtNodeTypes.STRING_TEMPLATE_ID -> convertStringTemplate(node)
            in constantExpressionsId -> convertConstantExpression(node)
            KtNodeTypes.REFERENCE_EXPRESSION_ID -> convertSimpleNameExpression(node)
            KtNodeTypes.FOR_ID -> convertFor(node) // FirBlock
            KtNodeTypes.TRY_ID -> convertTryExpression(node)
            KtNodeTypes.IF_ID -> convertIfExpression(node)
            KtNodeTypes.BREAK_ID, KtNodeTypes.CONTINUE_ID -> convertLoopJump(node)
            KtNodeTypes.RETURN_ID -> convertReturn(node)
            KtNodeTypes.THROW_ID -> convertThrow(node)
            KtNodeTypes.PARENTHESIZED_ID -> {
                val content = node.getExpressionInParentheses()
                context.forwardLabelUsagePermission(node, content)
                getAsFirExpression(content, "Empty parentheses", sourceWhenInvalidExpression = node)
            }
            KtNodeTypes.PROPERTY_DELEGATE_ID, KtNodeTypes.INDICES_ID, KtNodeTypes.CONDITION_ID, KtNodeTypes.LOOP_RANGE_ID ->
                getAsFirExpression(node.getFirstChildExpression(), errorReason, sourceWhenInvalidExpression = node)
            KtNodeTypes.THIS_EXPRESSION_ID -> convertThisExpression(node)
            KtNodeTypes.SUPER_EXPRESSION_ID -> convertSuperExpression(node)
            KtNodeTypes.OBJECT_LITERAL_ID -> declarationBuilder.convertObjectLiteral(node)
            KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> declarationBuilder.convertDestructingDeclaration(node)
                .toFirDestructingDeclaration(this, context, baseModuleData) // FirBlock

            // Sometimes non-expression FirStatement
            KtNodeTypes.BINARY_EXPRESSION_ID -> convertBinaryExpression(node)
            KtNodeTypes.LABELED_EXPRESSION_ID -> convertLabeledExpression(node)
            KtNodeTypes.ANNOTATED_EXPRESSION_ID -> convertAnnotatedExpression(node)
            KtNodeTypes.FUNCTION_ID -> declarationBuilder.convertFunctionDeclaration(node)

            // Always non-expression FirStatement
            KtNodeTypes.DO_WHILE_ID -> convertDoWhile(node)
            KtNodeTypes.WHILE_ID -> convertWhile(node)
            KtNodeTypes.PROPERTY_ID -> declarationBuilder.convertPropertyDeclaration(node)
            KtNodeTypes.CLASS_ID, KtNodeTypes.OBJECT_DECLARATION_ID -> declarationBuilder.convertClass(node)
            KtNodeTypes.TYPEALIAS_ID -> declarationBuilder.convertTypeAlias(node)

            else -> buildErrorExpression(
                node.toFirSourceElement(KtFakeSourceElementKind.ErrorTypeRef),
                ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
            )
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFunctionLiteral
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitLambdaExpression
     */
    private fun convertLambdaExpression(lambdaExpression: Node): FirAnonymousFunctionExpression {
        val valueParameterList = mutableListOf<ValueParameter<Node>>()
        var block: Node? = null
        var hasArrow = false

        val functionSymbol = FirAnonymousFunctionSymbol()
        lambdaExpression.getChildNodesByTokenId(KtNodeTypes.FUNCTION_LITERAL_ID).first().forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.VALUE_PARAMETER_LIST_ID -> valueParameterList += declarationBuilder.convertValueParameters(
                    valueParameters = it,
                    functionSymbol,
                    NodeTypeAnalyzer.ValueParameterDeclaration.LAMBDA
                )
                KtNodeTypes.BLOCK_ID -> block = it
                KtTokens.ARROW_ID -> hasArrow = true
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
                        context,
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

            body = context.withForcedLocalContext {
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
    private fun tryFoldStringConcatenation(binaryExpression: Node): FirExpression? {
        val input = mutableListOf<Node?>()
        val output = mutableListOf<Node?>()
        input.add(binaryExpression)
        while (input.isNotEmpty()) {
            val node = input.pop()
            when (node?.toTokenId()) {
                KtNodeTypes.BINARY_EXPRESSION_ID -> {
                    val [leftNode, operationReference, rightNode] = extractBinaryExpression(node)

                    if (operationReference.getOperationTokenId() != KtTokens.PLUS_ID) {
                        return null
                    }

                    input.add(leftNode)
                    input.add(rightNode)
                }
                else -> {
                    if (node?.toTokenId() != KtNodeTypes.STRING_TEMPLATE_ID) {
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

    private fun <T> MutableList<T>.pop(): T? {
        val result = lastOrNull()
        if (result != null) {
            removeAt(size - 1)
        }
        return result
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseBinaryExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitBinaryExpression
     */
    private fun convertBinaryExpression(binaryExpression: Node): FirStatement {
        return tryFoldStringConcatenation(binaryExpression) ?: convertBinaryExpressionFallback(binaryExpression)
    }

    private fun extractBinaryExpression(binaryExpression: Node): Triple<Node?, Node, Node?> {
        var left: Node? = null
        var op: Node? = null
        var right: Node? = null
        binaryExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.OPERATION_REFERENCE_ID -> {
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

    private fun convertBinaryExpressionFallback(binaryExpression: Node): FirStatement {
        val [leftArgNode, operationReference, rightArgNode] = extractBinaryExpression(binaryExpression)
        val operationReferenceSource = operationReference.toFirSourceElement()
        val operationTokenName = operationReference.asText
        val operationToken = operationReference.getOperationTokenId()
        val baseSource = binaryExpression.toFirSourceElement()
        if (operationToken == KtTokens.IDENTIFIER_ID) {
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
            KtTokens.ELVIS_ID ->
                return leftArgAsFir.generateNotNullOrOther(rightArgAsFir, baseSource)
            KtTokens.ANDAND_ID, KtTokens.OROR_ID ->
                return leftArgAsFir.generateLazyLogicalOperation(rightArgAsFir, operationToken == KtTokens.ANDAND_ID, baseSource)
            in inOperationsId ->
                return rightArgAsFir.generateContainsOperation(
                    leftArgAsFir, operationToken == KtTokens.NOT_IN_ID, baseSource, operationReferenceSource
                )
            in comparisonOperationsId ->
                return leftArgAsFir.generateComparisonExpression(rightArgAsFir, operationToken, baseSource, operationReferenceSource)
        }
        val conventionCallName = ktTokenToBinaryOperationNameMap[operationToken]
        return if (conventionCallName != null || operationToken == KtTokens.IDENTIFIER_ID) {
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
            val firOperation = ktTokenToFirOperationMap.getValue(operationToken)
            if (firOperation in FirOperation.ASSIGNMENTS) {
                leftArgNode.generateAssignment(
                    binaryExpression.toFirSourceElement(),
                    leftArgNode?.toFirSourceElement(),
                    rightArgAsFir,
                    firOperation,
                    leftArgAsFir.annotations,
                    rightArgNode,
                    leftArgNode?.toTokenId() in unwrappableTokenTypes,
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

    companion object {
        private val unwrappableTokenTypes: Set<Int> = setOf(
            KtNodeTypes.PARENTHESIZED_ID, KtNodeTypes.LABELED_EXPRESSION_ID, KtNodeTypes.ANNOTATED_EXPRESSION_ID
        )

        private val constantExpressionsId = setOf(
            KtNodeTypes.NULL_ID, KtNodeTypes.BOOLEAN_CONSTANT_ID, KtNodeTypes.FLOAT_CONSTANT_ID,
            KtNodeTypes.CHARACTER_CONSTANT_ID, KtNodeTypes.INTEGER_CONSTANT_ID
        )

        private val qualifiedAccessesId = setOf(KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID, KtNodeTypes.SAFE_ACCESS_EXPRESSION_ID)

        private val inOperationsId = setOf(KtTokens.IN_MODIFIER_ID, KtTokens.NOT_IN_ID)

        private val ktTokenToBinaryOperationNameMap: ImmutableBiMap<Int, Name> = ImmutableBiMap.builder<Int, Name>()
            .put(KtTokens.MUL_ID, OperatorNameConventions.TIMES)
            .put(KtTokens.PLUS_ID, OperatorNameConventions.PLUS)
            .put(KtTokens.MINUS_ID, OperatorNameConventions.MINUS)
            .put(KtTokens.DIV_ID, OperatorNameConventions.DIV)
            .put(KtTokens.PERC_ID, OperatorNameConventions.REM)
            .put(KtTokens.RANGE_ID, OperatorNameConventions.RANGE_TO)
            .put(KtTokens.RANGE_UNTIL_ID, OperatorNameConventions.RANGE_UNTIL)
            .build()

        private val ktTokenToUnaryOperationNameMap = ImmutableBiMap.builder<Int, Name>()
            .put(KtTokens.PLUSPLUS_ID, OperatorNameConventions.INC)
            .put(KtTokens.MINUSMINUS_ID, OperatorNameConventions.DEC)
            .put(KtTokens.PLUS_ID, OperatorNameConventions.UNARY_PLUS)
            .put(KtTokens.MINUS_ID, OperatorNameConventions.UNARY_MINUS)
            .put(KtTokens.EXCL_ID, OperatorNameConventions.NOT)
            .build()

        private val ktTokenToFirOperationMap = hashMapOf(
            KtTokens.LT_ID to FirOperation.LT,
            KtTokens.GT_ID to FirOperation.GT,
            KtTokens.LTEQ_ID to FirOperation.LT_EQ,
            KtTokens.GTEQ_ID to FirOperation.GT_EQ,
            KtTokens.EQEQ_ID to FirOperation.EQ,
            KtTokens.EXCLEQ_ID to FirOperation.NOT_EQ,
            KtTokens.EQEQEQ_ID to FirOperation.IDENTITY,
            KtTokens.EXCLEQEQEQ_ID to FirOperation.NOT_IDENTITY,

            KtTokens.EQ_ID to FirOperation.ASSIGN,
            KtTokens.PLUSEQ_ID to FirOperation.PLUS_ASSIGN,
            KtTokens.MINUSEQ_ID to FirOperation.MINUS_ASSIGN,
            KtTokens.MULTEQ_ID to FirOperation.TIMES_ASSIGN,
            KtTokens.DIVEQ_ID to FirOperation.DIV_ASSIGN,
            KtTokens.PERCEQ_ID to FirOperation.REM_ASSIGN,

            KtTokens.IS_KEYWORD_ID to FirOperation.IS,
            KtTokens.NOT_IS_ID to FirOperation.NOT_IS,

            KtTokens.AS_KEYWORD_ID to FirOperation.AS,
            KtTokens.AS_SAFE_ID to FirOperation.SAFE_AS,
        )

        private val incrementOperations: Set<Int> = setOf(
            KtTokens.PLUSPLUS_ID,
            KtTokens.MINUSMINUS_ID,
        )
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.Precedence.parseRightHandSide
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitBinaryWithTypeRHSExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitIsExpression
     */
    private fun convertBinaryWithTypeRHSExpression(binaryExpression: Node): FirTypeOperatorCall {
        lateinit var operationReference: Node
        var leftArgAsFir: FirExpression? = null
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.OPERATION_REFERENCE_ID -> operationReference = it
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = declarationBuilder.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
            }
        }

        return buildTypeOperatorCall {
            source = binaryExpression.toFirSourceElement()
            operation = ktTokenToFirOperationMap.getValue(operationReference.getOperationTokenId())
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
    private fun convertLabeledExpression(labeledExpression: Node): FirStatement {
        var firExpression: FirStatement? = null
        var labelSource: KtSourceElement? = null
        var forbiddenLabelKind: ForbiddenLabelKind? = null

        val isRepetitiveLabel = labeledExpression.getLabeledExpression()?.toTokenId() == KtNodeTypes.LABELED_EXPRESSION_ID

        labeledExpression.forEachChildren {
            context.setNewLabelUserNode(it)
            when (it.toTokenId()) {
                KtNodeTypes.LABEL_QUALIFIER_ID -> {
                    val name = it.asText.dropLast(1)
                    labelSource = it.getChildNodesByTokenId(KtNodeTypes.LABEL_ID).single().toFirSourceElement()
                    context.addNewLabel(buildLabel(name, labelSource))
                    forbiddenLabelKind = getForbiddenLabelKind(name, isRepetitiveLabel)
                }
                KtNodeTypes.BLOCK_ID -> firExpression = declarationBuilder.convertBlock(it)
                KtNodeTypes.PROPERTY_ID -> firExpression = declarationBuilder.convertPropertyDeclaration(it)
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
    private fun convertUnaryExpression(unaryExpression: Node): FirExpression {
        var argument: Node? = null
        lateinit var operationReference: Node
        unaryExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.OPERATION_REFERENCE_ID -> {
                    operationReference = it
                }
                else -> if (it.isExpression()) argument = it
            }
        }

        val operationToken = operationReference.getOperationTokenId()
        val conventionCallName = ktTokenToUnaryOperationNameMap[operationToken]
        return when {
            operationToken == KtTokens.EXCLEXCL_ID -> {
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
                if (operationToken in incrementOperations) {
                    return generateIncrementOrDecrementBlock(
                        unaryExpression,
                        operationReference,
                        argument,
                        callName = conventionCallName,
                        prefix = unaryExpression.toTokenId() == KtNodeTypes.PREFIX_EXPRESSION_ID
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
    private fun convertAnnotatedExpression(annotatedExpression: Node): FirStatement {
        var firExpression: FirStatement? = null
        val firAnnotationList = mutableListOf<FirAnnotation>()
        annotatedExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.ANNOTATION_ID -> declarationBuilder.convertAnnotationTo(it, firAnnotationList)
                KtNodeTypes.ANNOTATION_ENTRY_ID -> firAnnotationList += declarationBuilder.convertAnnotationEntry(it)
                KtNodeTypes.BLOCK_ID -> firExpression = declarationBuilder.convertBlockExpression(it)
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
    private fun convertClassLiteralExpression(classLiteralExpression: Node): FirGetClassCall {
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
    private fun convertCallableReferenceExpression(callableReferenceExpression: Node): FirCallableReferenceAccess {
        var isReceiver = true
        var hasQuestionMarkAtLhs = false
        var firReceiverExpression: FirExpression? = null
        lateinit var namedReference: FirNamedReference
        var errorArgumentListNode: Node? = null

        for (child in callableReferenceExpression.getChildren()) {
            when (child.toTokenId()) {
                KtTokens.COLONCOLON_ID -> isReceiver = false
                KtTokens.QUEST_ID -> hasQuestionMarkAtLhs = true

                // In invalid code like `::foo(args)`, the argument list is parsed
                // inside an ERROR_ELEMENT child of the callable reference expression
                SyntaxElementTypesWithIds.NO_ID -> {
                    for (errorChild in child.getChildren()) {
                        if (errorChild.toTokenId() == KtNodeTypes.VALUE_ARGUMENT_LIST_ID) {
                            errorArgumentListNode = errorChild
                            break
                        }
                    }
                }

                else -> if (child.isExpression()) {
                    if (isReceiver) {
                        firReceiverExpression = getAsFirExpression(child, "Incorrect receiver expression")
                    } else {
                        namedReference = createSimpleNamedReference(child.toFirSourceElement(), child)
                    }
                }
            }
        }

        return buildCallableReferenceAccess {
            source = callableReferenceExpression.toFirSourceElement()
            calleeReference = namedReference
            explicitReceiver = firReceiverExpression
            this.hasQuestionMarkAtLhs = hasQuestionMarkAtLhs
            errorArgumentListNode?.let {
                errorArgumentList = buildArgumentList {
                    source = it.toFirSourceElement()
                    arguments += convertValueArguments(it)
                }
            }
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parsePostfixExpression
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitQualifiedExpression
     */
    private fun convertQualifiedExpression(dotQualifiedExpression: Node): FirExpression {
        var isSelector = false
        var isSafe = false
        var firSelector: FirExpression? = null
        var firReceiver: FirExpression? = null //before dot
        dotQualifiedExpression.forEachChildren {
            when (val tokenType = it.toTokenId()) {
                KtTokens.DOT_ID -> isSelector = true
                KtTokens.SAFE_ACCESS_ID -> {
                    isSafe = true
                    isSelector = true
                }
                else -> {
                    val isEffectiveSelector = isSelector && tokenType != SyntaxElementTypesWithIds.NO_ID
                    val firExpression =
                        getAsFirExpression<FirExpression>(it, "Incorrect ${if (isEffectiveSelector) "selector" else "receiver"} expression")
                    if (isEffectiveSelector) {
                        val callExpressionCallee = if (tokenType == KtNodeTypes.CALL_EXPRESSION_ID) it.getFirstChildExpressionUnwrapped() else null
                        firSelector =
                            if (tokenType == KtNodeTypes.REFERENCE_EXPRESSION_ID ||
                                (tokenType == KtNodeTypes.CALL_EXPRESSION_ID && callExpressionCallee?.toTokenId() != KtNodeTypes.LAMBDA_EXPRESSION_ID)
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

        return when (val selector = firSelector) {
            is FirQualifiedAccessExpression -> {
                if (isSafe) {
                    @OptIn(FirImplementationDetail::class)
                    selector.replaceSource(dotQualifiedExpression.toFirSourceElement(KtFakeSourceElementKind.DesugaredSafeCallExpression))
                    return selector.createSafeCall(
                        firReceiver!!,
                        dotQualifiedExpression.toFirSourceElement()
                    )
                }
                convertFirSelector(selector, dotQualifiedExpression.toFirSourceElement(), firReceiver!!)
            }
            is FirErrorExpression if firReceiver != null -> {
                buildQualifiedErrorAccessExpression {
                    this.receiver = firReceiver
                    this.selector = selector
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
    private fun convertCallExpression(callSuffix: Node): FirExpression {
        var name: String? = null
        val firTypeArguments = mutableListOf<FirTypeProjection>()
        val valueArguments = mutableListOf<Node>()
        var additionalArgument: FirExpression? = null
        var hasArguments = false
        var superNode: Node? = null
        callSuffix.forEachChildren { child ->
            fun process(node: Node) {
                when (node.toTokenId()) {
                    KtNodeTypes.REFERENCE_EXPRESSION_ID -> {
                        name = node.asText
                    }
                    KtNodeTypes.SUPER_EXPRESSION_ID -> {
                        superNode = node
                    }
                    KtNodeTypes.PARENTHESIZED_ID -> if (node.toTokenId() != SyntaxElementTypesWithIds.NO_ID) {
                        additionalArgument = getAsFirExpression(
                            node.getExpressionInParentheses(),
                            "Incorrect invoke receiver",
                            sourceWhenInvalidExpression = node
                        )
                    }
                    KtNodeTypes.TYPE_ARGUMENT_LIST_ID -> {
                        firTypeArguments += declarationBuilder.convertTypeArguments(node, allowedUnderscoredTypeArgument = true)
                    }
                    KtNodeTypes.VALUE_ARGUMENT_LIST_ID, KtNodeTypes.LAMBDA_ARGUMENT_ID -> {
                        hasArguments = true
                        valueArguments += node
                    }
                    else -> if (node.toTokenId() != SyntaxElementTypesWithIds.NO_ID) {
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
            valueArguments.singleOrNull()?.toTokenId() == KtNodeTypes.LAMBDA_ARGUMENT_ID &&
            firTypeArguments.isEmpty()
        ) {
            valueArguments.single().getFirstChild()?.let {
                return getAsFirExpression<FirAnonymousFunctionExpression>(it).apply {
                    anonymousFunction.replaceStatus(anonymousFunction.status.copy(isSuspend = true))
                }
            }
        }

        (val calleeReference = reference, val receiverForInvoke) = when {
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
    private fun convertStringTemplate(stringTemplate: Node): FirExpression {
        val children = stringTemplate.getChildren()
        return children.toInterpolatingCall(
            stringTemplate,
            convertTemplateEntry = { convertShortOrLongStringTemplate(it) },
            prefix = { children.firstOrNull { it.toTokenId() == KtNodeTypes.STRING_INTERPOLATION_PREFIX_ID }?.asText ?: "" }
        )
    }

    private fun Node?.convertShortOrLongStringTemplate(errorReason: String): Collection<FirExpression> {
        val firExpressions = mutableListOf<FirExpression>()
        this?.forEachChildren {
            when (it.toTokenId()) {
                KtTokens.LONG_TEMPLATE_ENTRY_START_ID, KtTokens.LONG_TEMPLATE_ENTRY_END_ID, KtTokens.SHORT_TEMPLATE_ENTRY_START_ID -> return@forEachChildren
                else -> firExpressions.add(getAsFirExpression(it, errorReason))
            }
        }
        return firExpressions
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseLiteralConstant
     */
    private fun convertConstantExpression(constantExpression: Node): FirExpression {
        return generateConstantExpressionByLiteral(constantExpression)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhen
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitWhenExpression
     */
    private fun convertWhenExpression(whenExpression: Node): FirWhenExpression {
        var subjectExpression: FirExpression? = null
        var subjectVariable: FirVariable? = null
        val whenEntryNodes = mutableListOf<Node>()
        val whenEntries = mutableListOf<WhenEntry<Node>>()
        whenExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.PROPERTY_ID -> {
                    subjectVariable = (declarationBuilder.convertPropertyDeclaration(it) as FirVariable).let { variable ->
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
                }
                KtNodeTypes.DESTRUCTURING_DECLARATION_ID -> subjectExpression =
                    getAsFirExpression(it, "Incorrect when subject expression: ${whenExpression.asText}")
                KtNodeTypes.WHEN_ENTRY_ID -> whenEntryNodes += it
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
        whenEntry: Node,
        subjectVariable: FirVariable?,
    ): WhenEntry<Node> {
        var isElse = false
        var firBlock: FirBlock = buildEmptyExpressionBlock()
        val conditions = mutableListOf<FirExpression>()
        var guard: FirExpression? = null
        var shouldBindSubject = false
        whenEntry.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.WHEN_CONDITION_EXPRESSION_ID -> conditions += convertWhenConditionExpression(it, subjectVariable)
                KtNodeTypes.WHEN_CONDITION_IN_RANGE_ID -> {
                    (val condition = expression, val shouldBind = shouldBindSubject) = convertWhenConditionInRange(it, subjectVariable)
                    conditions += condition
                    shouldBindSubject = shouldBindSubject || shouldBind
                }
                KtNodeTypes.WHEN_CONDITION_IS_PATTERN_ID -> {
                    (val condition = expression, val shouldBind = shouldBindSubject) = convertWhenConditionIsPattern(it, subjectVariable)
                    conditions += condition
                    shouldBindSubject = shouldBindSubject || shouldBind
                }
                KtNodeTypes.WHEN_ENTRY_GUARD_ID -> guard = getAsFirExpression(
                    it.getFirstChildExpressionUnwrapped(),
                    "No expression in guard",
                    sourceWhenInvalidExpression = it
                )
                KtTokens.ELSE_KEYWORD_ID -> isElse = true
                KtNodeTypes.BLOCK_ID -> firBlock = declarationBuilder.convertBlock(it)
                else -> if (it.isExpression()) firBlock = declarationBuilder.convertBlock(it)
            }
        }

        return WhenEntry(conditions, guard, firBlock, whenEntry, isElse, shouldBindSubject, whenEntry.toFirSourceElement())
    }

    private fun convertWhenConditionExpression(
        whenCondition: Node,
        subjectVariable: FirVariable?,
    ): FirExpression {
        var firExpression: FirExpression? = null
        whenCondition.forEachChildren {
            when (it.toTokenId()) {
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
        whenCondition: Node,
        subjectVariable: FirVariable?,
    ): WhenConditionConvertedResults {
        var isNegate = false
        var firExpression: FirExpression? = null
        var conditionSource: KtLightSourceElement? = null
        whenCondition.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.OPERATION_REFERENCE_ID if it.getOperationTokenId() == KtTokens.NOT_IN_ID -> {
                    conditionSource = it.toFirSourceElement() as KtLightSourceElement
                    isNegate = true
                }
                KtNodeTypes.OPERATION_REFERENCE_ID -> {
                    conditionSource = it.toFirSourceElement() as KtLightSourceElement
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
        whenCondition: Node,
        subjectVariable: FirVariable?,
    ): WhenConditionConvertedResults {
        lateinit var firOperation: FirOperation
        var firType: FirTypeRef? = null
        whenCondition.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.TYPE_REFERENCE_ID -> firType = declarationBuilder.convertType(it)
                KtTokens.IS_KEYWORD_ID -> firOperation = FirOperation.IS
                KtTokens.NOT_IS_ID -> firOperation = FirOperation.NOT_IS
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
        whenCondition: Node,
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
    private fun convertArrayAccessExpression(arrayAccess: Node): FirExpression {
        var firExpression: FirExpression? = null
        val indices: MutableList<FirExpression> = mutableListOf()
        arrayAccess.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.INDICES_ID -> indices += convertIndices(it)
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
    private fun convertCollectionLiteralExpression(expression: Node): FirCollectionLiteral {
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
    private fun convertIndices(indices: Node): List<FirExpression> {
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
    private fun convertSimpleNameExpression(referenceExpression: Node): FirQualifiedAccessExpression {
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
        referenceExpression: Node,
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
    private fun convertDoWhile(doWhileLoop: Node): FirLoop {
        var block: Node? = null
        var firCondition: FirExpression? = null

        val target: FirLoopTarget
        return FirDoWhileLoopBuilder().apply {
            source = doWhileLoop.toFirSourceElement()
            // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
            target = prepareTarget(doWhileLoop)
            doWhileLoop.forEachChildren {
                when (it.toTokenId()) {
                    KtNodeTypes.BODY_ID -> block = it
                    KtNodeTypes.CONDITION_ID -> firCondition = getAsFirExpression(it, "No condition in do-while loop")
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
    private fun convertWhile(whileLoop: Node): FirLoop {
        var block: Node? = null
        var firCondition: FirExpression? = null
        whileLoop.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.BODY_ID -> block = it
                KtNodeTypes.CONDITION_ID -> firCondition = getAsFirExpression(it, "No condition in while loop")
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
    private fun convertFor(forLoop: Node): FirBlock {
        var parameter: ValueParameter<Node>? = null
        var rangeExpression: FirExpression? = null
        var blockNode: Node? = null
        forLoop.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.VALUE_PARAMETER_ID -> parameter = declarationBuilder.convertValueParameter(it, null, NodeTypeAnalyzer.ValueParameterDeclaration.FOR_LOOP)
                KtNodeTypes.LOOP_RANGE_ID -> rangeExpression = getAsFirExpression(it, "No range in for loop")
                KtNodeTypes.BODY_ID -> blockNode = it
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
                    val quotedName = valueParameter.source.toNode().getChildNodeByTokenId(KtTokens.IDENTIFIER_ID)?.asText
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
                            context,
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
    private fun convertLoopBody(body: Node?): FirBlock {
        return convertLoopOrIfBody(body) ?: buildEmptyExpressionBlock()
    }

    private fun convertLoopOrIfBody(body: Node?): FirBlock? {
        var firBlock: FirBlock? = null
        var firStatement: FirStatement? = null
        body?.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.BLOCK_ID -> firBlock = declarationBuilder.convertBlockExpression(it)
                KtNodeTypes.ANNOTATED_EXPRESSION_ID -> {
                    if (it.getChildNodeByTokenId(KtNodeTypes.BLOCK_ID) != null) {
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
    private fun convertTryExpression(tryExpression: Node): FirTryExpression {
        lateinit var tryBlock: FirBlock
        val catchClauses = mutableListOf<Triple<ValueParameter<Node>?, FirBlock, KtLightSourceElement>>()
        var finallyBlock: FirBlock? = null
        tryExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.BLOCK_ID -> tryBlock = declarationBuilder.convertBlock(it)
                KtNodeTypes.CATCH_ID -> convertCatchClause(it)?.also { oneClause -> catchClauses += oneClause }
                KtNodeTypes.FINALLY_ID -> finallyBlock = convertFinally(it)
            }
        }
        return buildTryExpression {
            source = tryExpression.toFirSourceElement()
            this.tryBlock = tryBlock
            this.finallyBlock = finallyBlock
            for ([parameter, block, clauseSource] in catchClauses) {
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
    private fun convertCatchClause(catchClause: Node): Triple<ValueParameter<Node>?, FirBlock, KtLightSourceElement>? {
        var valueParameter: ValueParameter<Node>? = null
        var blockNode: Node? = null
        var emptyValueParameterList = false
        catchClause.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.VALUE_PARAMETER_LIST_ID -> valueParameter = declarationBuilder.convertValueParameters(
                    valueParameters = it,
                    FirAnonymousFunctionSymbol(),
                    NodeTypeAnalyzer.ValueParameterDeclaration.CATCH
                ).firstOrNull() ?: run { emptyValueParameterList = true; null }

                KtNodeTypes.BLOCK_ID -> blockNode = it
            }
        }

        if (emptyValueParameterList) return null
        return Triple(valueParameter, declarationBuilder.convertBlock(blockNode), catchClause.toFirSourceElement() as KtLightSourceElement)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseTry
     */
    private fun convertFinally(finallyExpression: Node): FirBlock {
        var blockNode: Node? = null
        finallyExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.BLOCK_ID -> blockNode = it
            }
        }

        return declarationBuilder.convertBlock(blockNode)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseIf
     * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.visitIfExpression
     */
    private fun convertIfExpression(ifExpression: Node): FirWhenExpression {
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

    private inner class IfNodeComponents(val firCondition: FirExpression?, val thenBlock: Node?, val elseBlock: Node?)

    private fun parseIfExpression(ifExpression: Node): IfNodeComponents {
        var firCondition: FirExpression? = null
        var thenBlock: Node? = null
        var elseBlock: Node? = null
        ifExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.CONDITION_ID -> firCondition = getAsFirExpression(it, "If statement should have condition")
                KtNodeTypes.THEN_ID -> thenBlock = it
                KtNodeTypes.ELSE_ID -> elseBlock = it
            }
        }
        return IfNodeComponents(firCondition, thenBlock, elseBlock)
    }

    private val Node.usedAsExpression: Boolean
        get() {
            var parent = getParent() ?: return true
            while (parent.toTokenId() == KtNodeTypes.ANNOTATED_EXPRESSION_ID ||
                parent.toTokenId() == KtNodeTypes.LABELED_EXPRESSION_ID
            ) {
                parent = parent.getParent() ?: return true
            }
            val parentTokenType = parent.toTokenId()
            return when (parentTokenType) {
                KtNodeTypes.BLOCK_ID -> parent.getLastChildExpression() == this && parent.usedAsExpression
                KtNodeTypes.TRY_ID, KtNodeTypes.CATCH_ID -> parent.usedAsExpression
                KtNodeTypes.THEN_ID, KtNodeTypes.ELSE_ID, KtNodeTypes.WHEN_ENTRY_ID -> parent.getParent()?.usedAsExpression ?: true
                KtNodeTypes.CLASS_INITIALIZER_ID, KtNodeTypes.SCRIPT_INITIALIZER_ID, KtNodeTypes.SECONDARY_CONSTRUCTOR_ID, KtNodeTypes.FUNCTION_LITERAL_ID, KtNodeTypes.FINALLY_ID -> false
                KtNodeTypes.FUNCTION_ID, KtNodeTypes.PROPERTY_ACCESSOR_ID -> parent.getChildren().any { it.toTokenId() == KtTokens.EQ_ID }
                KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID -> parent.getFirstChild() == this
                KtNodeTypes.BODY_ID -> when (parent.getParent()?.toTokenId()) {
                    KtNodeTypes.FOR_ID, KtNodeTypes.WHILE_ID, KtNodeTypes.DO_WHILE_ID -> false
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
    private fun convertLoopJump(jump: Node): FirLoopJump {
        var isBreak = true
        jump.forEachChildren {
            when (it.toTokenId()) {
                KtTokens.CONTINUE_KEYWORD_ID -> isBreak = false
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
    private fun convertReturn(returnExpression: Node): FirReturnExpression {
        var labelName: String? = null
        var firExpression: FirExpression? = null
        returnExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.LABEL_QUALIFIER_ID -> labelName = it.getAsStringWithoutBacktick().replace("@", "")
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
    private fun convertThrow(throwExpression: Node): FirThrowExpression {
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
    private fun convertThisExpression(thisExpression: Node): FirThisReceiverExpression {
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
    private fun convertSuperExpression(superExpression: Node): FirSuperReceiverExpression {
        val label: String? = superExpression.getLabelName()
        var superTypeRef: FirTypeRef = implicitType
        superExpression.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.TYPE_REFERENCE_ID -> superTypeRef = declarationBuilder.convertType(it)
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
    fun convertValueArguments(valueArguments: Node): List<FirExpression> {
        return valueArguments.forEachChildrenReturnList { node, container ->
            @Suppress("IncorrectFormatting")
            when (node.toTokenId()) {
                KtNodeTypes.VALUE_ARGUMENT_ID -> container += convertValueArgument(node)
                KtNodeTypes.LAMBDA_EXPRESSION_ID,
                KtNodeTypes.LABELED_EXPRESSION_ID,
                KtNodeTypes.ANNOTATED_EXPRESSION_ID -> container += getAsFirExpression<FirAnonymousFunctionExpression>(node).apply {
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
    private fun convertValueArgument(valueArgument: Node): FirExpression {
        var identifier: String? = null
        var isSpread = false
        var firExpression: FirExpression? = null
        valueArgument.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.VALUE_ARGUMENT_NAME_ID -> identifier = it.asText
                KtTokens.MUL_ID -> isSpread = true
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

    private fun Node.isExpression(): Boolean = toTokenId().isExpression()

    private fun Node.getExpressionInParentheses(): Node? = getFirstChildExpression()

    private fun Node.getLabeledExpression(): Node? = getLastChildExpression()

    private fun getForbiddenLabelKind(rawName: String, isMultipleLabel: Boolean): ForbiddenLabelKind? = when {
        rawName.isUnderscore -> ForbiddenLabelKind.UNDERSCORE_IS_RESERVED
        isMultipleLabel -> ForbiddenLabelKind.MULTIPLE_LABEL
        else -> null
    }
}
