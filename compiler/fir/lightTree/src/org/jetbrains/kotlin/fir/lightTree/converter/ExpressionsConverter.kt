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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.impl.FirLabelImpl
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.WhenEntry
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
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

    inline fun <reified R : FirElement> getAsFirExpression(expression: LighterASTNode?, errorReason: String = ""): R {
        return expression?.let { convertExpression(it, errorReason) } as? R ?: (FirErrorExpressionImpl(null, FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax)) as R)
    }

    /*****    EXPRESSIONS    *****/
    fun convertExpression(expression: LighterASTNode, errorReason: String): FirElement {
        if (!stubMode) {
            return when (expression.tokenType) {
                LAMBDA_EXPRESSION -> {
                    val lambdaTree = LightTree2Fir.buildLightTreeLambdaExpression(expression.asText)
                    ExpressionsConverter(session, stubMode, lambdaTree, declarationsConverter, context)
                        .convertLambdaExpression(lambdaTree.root)
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
                else -> FirErrorExpressionImpl(null, FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax))
            }
        }

        return FirExpressionStub(null)
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

        return FirAnonymousFunctionImpl(null, session, implicitType, implicitType, FirAnonymousFunctionSymbol(), isLambda = true).apply {
            context.firFunctions += this
            var destructuringBlock: FirExpression? = null
            for (valueParameter in valueParameterList) {
                val multiDeclaration = valueParameter.destructuringDeclaration
                valueParameters += if (multiDeclaration != null) {
                    val name = Name.special("<destruct>")
                    val multiParameter = FirValueParameterImpl(
                        null,
                        this@ExpressionsConverter.session,
                        FirImplicitTypeRefImpl(null),
                        name,
                        FirVariableSymbol(name),
                        defaultValue = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isVararg = false
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
                FirLabelImpl(null, it.asString())
            }
            val bodyExpression = block?.let { declarationsConverter.convertBlockExpression(it) }
                ?: FirErrorExpressionImpl(null, FirSimpleDiagnostic("Lambda has no body", DiagnosticKind.Syntax))
            body = if (bodyExpression is FirBlockImpl) {
                if (bodyExpression.statements.isEmpty()) {
                    bodyExpression.statements.add(FirUnitExpression(null))
                }
                if (destructuringBlock is FirBlock) {
                    for ((index, statement) in destructuringBlock.statements.withIndex()) {
                        bodyExpression.statements.add(index, statement)
                    }
                }
                bodyExpression
            } else {
                FirSingleExpressionBlock(bodyExpression.toReturn())
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
        var rightArgAsFir: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No right operand", DiagnosticKind.Syntax))
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
                    rightArgAsFir, operationToken == ANDAND, null
                )
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgAsFir.generateContainsOperation(
                    getAsFirExpression(leftArgNode, "No left operand"), operationToken == NOT_IN, null, null
                )
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            FirFunctionCallImpl(null).apply {
                calleeReference = FirSimpleNamedReference(
                    null, conventionCallName ?: operationTokenName.nameAsSafeName(), null
                )
                explicitReceiver = getAsFirExpression(leftArgNode, "No left operand")
                arguments += rightArgAsFir
            }
        } else {
            val firOperation = operationToken.toFirOperation()
            if (firOperation in FirOperation.ASSIGNMENTS) {
                return leftArgNode.generateAssignment(null, rightArgAsFir, firOperation) { getAsFirExpression(this) }
            } else {
                FirOperatorCallImpl(null, firOperation).apply {
                    arguments += getAsFirExpression<FirExpression>(leftArgNode, "No left operand")
                    arguments += rightArgAsFir
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
        var leftArgAsFir: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No left operand", DiagnosticKind.Syntax))
        lateinit var firType: FirTypeRef
        binaryExpression.forEachChildren {
            when (it.tokenType) {
                OPERATION_REFERENCE -> operationTokenName = it.asText
                TYPE_REFERENCE -> firType = declarationsConverter.convertType(it)
                else -> if (it.isExpression()) leftArgAsFir = getAsFirExpression(it, "No left operand")
            }
        }

        val operation = operationTokenName.toFirOperation()
        return FirTypeOperatorCallImpl(null, operation, firType).apply {
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
                LABEL_QUALIFIER -> context.firLabels += FirLabelImpl(null, it.toString().replace("@", ""))
                BLOCK -> firExpression = declarationsConverter.convertBlock(it)
                PROPERTY -> firExpression = declarationsConverter.convertPropertyDeclaration(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        if (size != context.firLabels.size) {
            context.firLabels.removeLast()
            //println("Unused label: ${labeledExpression.getAsString()}")
        }
        return firExpression ?: FirErrorExpressionImpl(null, FirSimpleDiagnostic("Empty label", DiagnosticKind.Syntax))
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
        val conventionCallName = operationToken.toUnaryName()
        return when {
            operationToken == EXCLEXCL -> {
                FirCheckNotNullCallImpl(null).apply {
                    arguments += getAsFirExpression<FirExpression>(argument, "No operand")
                }

            }
            conventionCallName != null -> {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        null,
                        argument,
                        callName = conventionCallName,
                        prefix = unaryExpression.tokenType == PREFIX_EXPRESSION
                    ) { getAsFirExpression(this) }
                }
                FirFunctionCallImpl(null).apply {
                    calleeReference = FirSimpleNamedReference(null, conventionCallName, null)
                    explicitReceiver = getAsFirExpression(argument, "No operand")
                }
            }
            else -> {
                val firOperation = operationToken.toFirOperation()
                FirOperatorCallImpl(null, firOperation).apply {
                    arguments += getAsFirExpression<FirExpression>(argument, "No operand")
                }
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
        } ?: FirErrorExpressionImpl(null, FirSimpleDiagnostic("Strange annotated expression: ${firExpression?.render()}", DiagnosticKind.Syntax))
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoubleColonSuffix
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitClassLiteralExpression
     */
    private fun convertClassLiteralExpression(classLiteralExpression: LighterASTNode): FirExpression {
        var firReceiverExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No receiver in class literal", DiagnosticKind.Syntax))
        classLiteralExpression.forEachChildren {
            if (it.isExpression()) firReceiverExpression = getAsFirExpression(it, "No receiver in class literal")
        }

        return FirGetClassCallImpl(null).apply {
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

        return FirCallableReferenceAccessImpl(null).apply {
            calleeReference = firCallableReference.calleeReference as FirNamedReference
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
        var firSelector: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax)) //after dot
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

        (firSelector as? FirModifiableQualifiedAccess)?.let {
            it.safe = isSafe
            it.explicitReceiver = firReceiver
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
        callSuffix.forEachChildren { child ->
            fun process(node: LighterASTNode) {
                when (node.tokenType) {
                    REFERENCE_EXPRESSION -> {
                        name = node.asText
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

        val (calleeReference, explicitReceiver) = when {
            name != null -> FirSimpleNamedReference(null, name.nameAsSafeName(), null) to null
            additionalArgument != null -> {
                FirSimpleNamedReference(null, OperatorNameConventions.INVOKE, null) to additionalArgument!!
            }
            else -> FirErrorNamedReferenceImpl(null, FirSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax)) to null
        }

        return if (hasArguments) {
            FirFunctionCallImpl(null).apply {
                this.calleeReference = calleeReference

                context.firFunctionCalls += this
                this.extractArgumentsFrom(valueArguments.flatMap { convertValueArguments(it) }, stubMode)
                context.firFunctionCalls.removeLast()
            }
        } else {
            FirQualifiedAccessExpressionImpl(null).apply {
                this.calleeReference = calleeReference
            }
        }.apply {
            this.explicitReceiver = explicitReceiver
            typeArguments += firTypeArguments
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseStringTemplate
     */
    private fun convertStringTemplate(stringTemplate: LighterASTNode): FirExpression {
        return stringTemplate.getChildrenAsArray().toInterpolatingCall(null) { convertShortOrLongStringTemplate(it) }
    }

    private fun LighterASTNode?.convertShortOrLongStringTemplate(errorReason: String): FirExpression {
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax))
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
                    FirPropertyImpl(
                        null,
                        session,
                        variable.returnTypeRef,
                        null,
                        variable.name,
                        variable.initializer,
                        null,
                        false,
                        FirPropertySymbol(variable.name),
                        true,
                        FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
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
        return FirWhenExpressionImpl(null, subjectExpression, subjectVariable).apply {
            if (hasSubject) {
                subject.bind(this)
            }
            for (entry in whenEntries) {
                val branch = entry.firBlock
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        val firCondition = entry.toFirWhenCondition(subject)
                        FirWhenBranchImpl(null, firCondition, branch)
                    } else {
                        val firCondition = entry.toFirWhenConditionWithoutSubject()
                        FirWhenBranchImpl(null, firCondition, branch)
                    }
                } else {
                    FirWhenBranchImpl(
                        null, FirElseIfTrueCondition(null), branch
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
        var firBlock: FirBlock = FirEmptyExpressionBlock()
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
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No expression in condition with expression", DiagnosticKind.Syntax))
        whenCondition.forEachChildren {
            when (it.tokenType) {
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No expression in condition with expression")
            }
        }

        return FirOperatorCallImpl(null, FirOperation.EQ).apply {
            arguments += firExpression
        }
    }

    private fun convertWhenConditionInRange(whenCondition: LighterASTNode): FirExpression {
        var isNegate = false
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No range in condition with range", DiagnosticKind.Syntax))
        whenCondition.forEachChildren {
            when (it.tokenType) {
                NOT_IN -> isNegate = true
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it)
            }
        }

        val name = if (isNegate) OperatorNameConventions.NOT else SpecialNames.NO_NAME_PROVIDED
        return FirFunctionCallImpl(null).apply {
            calleeReference = FirSimpleNamedReference(null, name, null)
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

        return FirTypeOperatorCallImpl(null, firOperation, firType)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseArrayAccess
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitArrayAccessExpression
     */
    private fun convertArrayAccessExpression(arrayAccess: LighterASTNode): FirFunctionCall {
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No array expression", DiagnosticKind.Syntax))
        val indices: MutableList<FirExpression> = mutableListOf()
        arrayAccess.forEachChildren {
            when (it.tokenType) {
                INDICES -> indices += convertIndices(it)
                else -> if (it.isExpression()) firExpression = getAsFirExpression(it, "No array expression")
            }
        }
        return FirFunctionCallImpl(null).apply {
            calleeReference = FirSimpleNamedReference(null, OperatorNameConventions.GET, null)
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

        return FirArrayOfCallImpl(null).apply {
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
        return FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference =
                FirSimpleNamedReference(null, referenceExpression.asText.nameAsSafeName(), null)
        }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseDoWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitDoWhileExpression
     */
    private fun convertDoWhile(doWhileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No condition in do-while loop", DiagnosticKind.Syntax))
        doWhileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in do-while loop")
            }
        }

        return FirDoWhileLoopImpl(null, firCondition).configure { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseWhile
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitWhileExpression
     */
    private fun convertWhile(whileLoop: LighterASTNode): FirElement {
        var block: LighterASTNode? = null
        var firCondition: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No condition in while loop", DiagnosticKind.Syntax))
        whileLoop.forEachChildren {
            when (it.tokenType) {
                BODY -> block = it
                CONDITION -> firCondition = getAsFirExpression(it, "No condition in while loop")
            }
        }

        return FirWhileLoopImpl(null, firCondition).configure { convertLoopBody(block) }
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseFor
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitForExpression
     */
    private fun convertFor(forLoop: LighterASTNode): FirElement {
        var parameter: ValueParameter? = null
        var rangeExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("No range in for loop", DiagnosticKind.Syntax))
        var blockNode: LighterASTNode? = null
        forLoop.forEachChildren {
            when (it.tokenType) {
                VALUE_PARAMETER -> parameter = declarationsConverter.convertValueParameter(it)
                LOOP_RANGE -> rangeExpression = getAsFirExpression(it, "No range in for loop")
                BODY -> blockNode = it
            }
        }

        return FirBlockImpl(null).apply {
            val rangeVal =
                generateTemporaryVariable(this@ExpressionsConverter.session, null, Name.special("<range>"), rangeExpression)
            statements += rangeVal
            val iteratorVal = generateTemporaryVariable(
                this@ExpressionsConverter.session, null, Name.special("<iterator>"),
                FirFunctionCallImpl(null).apply {
                    calleeReference = FirSimpleNamedReference(null, Name.identifier("iterator"), null)
                    explicitReceiver = generateResolvedAccessExpression(null, rangeVal)
                }
            )
            statements += iteratorVal
            statements += FirWhileLoopImpl(
                null,
                FirFunctionCallImpl(null).apply {
                    calleeReference = FirSimpleNamedReference(null, Name.identifier("hasNext"), null)
                    explicitReceiver = generateResolvedAccessExpression(null, iteratorVal)
                }
            ).configure {
                // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                val block = FirBlockImpl(null).apply {
                    statements += convertLoopBody(blockNode).statements
                }
                if (parameter != null) {
                    val multiDeclaration = parameter!!.destructuringDeclaration
                    val firLoopParameter = generateTemporaryVariable(
                        this@ExpressionsConverter.session, null,
                        if (multiDeclaration != null) Name.special("<destruct>") else parameter!!.firValueParameter.name,
                        FirFunctionCallImpl(null).apply {
                            calleeReference = FirSimpleNamedReference(null, Name.identifier("next"), null)
                            explicitReceiver = generateResolvedAccessExpression(null, iteratorVal)
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
            firStatement != null -> FirSingleExpressionBlock(firStatement!!)
            firBlock == null -> FirEmptyExpressionBlock()
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
        return FirTryExpressionImpl(null, tryBlock, finallyBlock).apply {
            for ((parameter, block) in catchClauses) {
                if (parameter == null) continue
                catches += FirCatchImpl(null, parameter.firValueParameter, block)
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
        var firCondition: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("If statement should have condition", DiagnosticKind.Syntax))
        var thenBlock: LighterASTNode? = null
        var elseBlock: LighterASTNode? = null
        ifExpression.forEachChildren {
            when (it.tokenType) {
                CONDITION -> firCondition = getAsFirExpression(it, "If statement should have condition")
                THEN -> thenBlock = it
                ELSE -> elseBlock = it
            }
        }

        return FirWhenExpressionImpl(null, null, null).apply {
            val trueBranch = convertLoopBody(thenBlock)
            branches += FirWhenBranchImpl(null, firCondition, trueBranch)
            val elseBranch = convertLoopBody(elseBlock)
            if (elseBranch !is FirEmptyExpressionBlock) {
                branches += FirWhenBranchImpl(
                    null, FirElseIfTrueCondition(null), elseBranch
                )
            }
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

        return (if (isBreak) FirBreakExpressionImpl(null) else FirContinueExpressionImpl(null)).bindLabel(jump)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseReturn
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitReturnExpression
     */
    private fun convertReturn(returnExpression: LighterASTNode): FirExpression {
        var labelName: String? = null
        var firExpression: FirExpression = FirUnitExpression(null)
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
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("Nothing to throw", DiagnosticKind.Syntax))
        throwExpression.forEachChildren {
            if (it.isExpression()) firExpression = getAsFirExpression(it, "Nothing to throw")
        }

        return FirThrowExpressionImpl(null, firExpression)
    }

    /**
     * @see org.jetbrains.kotlin.parsing.KotlinExpressionParsing.parseThisExpression
     * @see org.jetbrains.kotlin.fir.builder.RawFirBuilder.Visitor.visitThisExpression
     */
    private fun convertThisExpression(thisExpression: LighterASTNode): FirQualifiedAccessExpression {
        val label: String? = thisExpression.getLabelName()
        return FirThisReceiverExpressionImpl(null, FirExplicitThisReference(null, label))
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

        return FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference = FirExplicitSuperReference(null, superTypeRef)
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
                ANNOTATED_EXPRESSION -> container += FirLambdaArgumentExpressionImpl(null, getAsFirExpression(node))
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
        var firExpression: FirExpression = FirErrorExpressionImpl(null, FirSimpleDiagnostic("Argument is absent", DiagnosticKind.Syntax))
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
            identifier != null -> FirNamedArgumentExpressionImpl(
                null,
                firExpression,
                isSpread,
                identifier.nameAsSafeName()
            )
            isSpread -> FirSpreadArgumentExpressionImpl(null, firExpression)
            else -> firExpression
        }
    }
}
