/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.NodeTypeAnalyzer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.componentFunctionSymbol
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.parsing.*
import org.jetbrains.kotlin.psi.utils.hasIllegallyPositionedUnderscore
import org.jetbrains.kotlin.psi.utils.parseNumericLiteral
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Abstract class for all FIR builders.
 *
 * [Node] can be either PsiElement, LighterASTNode or LightNode
 * [Type] can be either IElementType or SyntaxElementType
 */
abstract class AbstractRawFirBuilder<Node : Any, Type : Any>(
    val baseSession: FirSession,
    val context: Context<Node> = Context(),
) : NodeTypeAnalyzer<Node, Type> {
    companion object {
        fun firScriptName(fileName: String): Name = Name.special("<script-$fileName>")
        fun firSnippetName(fileName: String): Name = Name.special("<snippet-$fileName>")
    }

    val baseModuleData: FirModuleData = baseSession.moduleData

    override val implicitType: FirImplicitTypeRef = FirImplicitTypeRefImplWithoutSource
    override val implicitUnitType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.unitType
    override val implicitAnyType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.anyType
    override val implicitEnumType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.enumType
    override val implicitAnnotationType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.annotationType

    protected val imitateLambdaSuspendModifier: Boolean =
        baseSession.languageVersionSettings.supportsFeature(LanguageFeature.ParseLambdaWithSuspendModifier)

    private val nameBasedDestructuringShortFormEnabled: Boolean =
        baseSession.languageVersionSettings.supportsFeature(LanguageFeature.EnableNameBasedDestructuringShortForm)

    override fun destructuringKindOf(hasSquareBrackets: Boolean, isFullForm: Boolean): DestructuringKind {
        return when {
            hasSquareBrackets -> DestructuringKind.PositionalWithSquareBrackets
            isFullForm || nameBasedDestructuringShortFormEnabled -> DestructuringKind.NameBased
            else -> DestructuringKind.PositionalWithParentheses
        }
    }

    abstract fun Node.getExpressionInParentheses(): Node?
    abstract fun Node.getAnnotatedExpression(): Node?
    abstract fun Node.getLabeledExpression(): Node?
    abstract val Node?.arrayExpression: Node?
    abstract val Node?.indexExpressions: List<Node>?
    abstract val Node.isVararg: Boolean

    override fun registerSelfType(selfType: FirResolvedTypeRef) {
        context.dispatchReceiverTypesStack.add(selfType.coneType as ConeClassLikeType)
    }

    inline fun <T> withContainerReplSymbol(
        symbol: FirReplSnippetSymbol,
        block: () -> T,
    ): T {
        require(context.containingReplSymbol == null) { "Nested snippets are not supported" }
        context.containingReplSymbol = symbol
        return try {
            block()
        } finally {
            context.containingReplSymbol = null
        }
    }

    val isDirectlyInsideCompanionBlock: Boolean
        get() = context.currentCompanionBlockOwnerOrNull.let { it != null && it == context.containerSymbolIfAny }

    override fun callableIdForName(name: Name): CallableId =
        when {
            context.className.shortNameOrSpecial() == SpecialNames.ANONYMOUS -> CallableId(
                ClassId(context.packageFqName, SpecialNames.ANONYMOUS_FQ_NAME, isLocal = true), name
            )
            context.className.isRoot && !context.inLocalContext -> CallableId(context.packageFqName, name)
            context.inLocalContext -> {
                val pathFqName =
                    context.firFunctionTargets.fold(
                        if (context.classNameBeforeLocalContext.isRoot) {
                            context.packageFqName
                        } else {
                            ClassId(context.packageFqName, context.classNameBeforeLocalContext, isLocal = false).asSingleFqName()
                        }
                    ) { result, firFunctionTarget ->
                        if (firFunctionTarget.isLambda || firFunctionTarget.labelName == null)
                            result
                        else
                            result.child(Name.identifier(firFunctionTarget.labelName!!))
                    }
                CallableId(name, pathFqName)
            }
            else -> CallableId(context.packageFqName, context.className, name)
        }

    fun currentDispatchReceiverType(): ConeClassLikeType? = currentDispatchReceiverType(context)

    /**
     * @return second from the end dispatch receiver. For the inner class constructor, it would be the outer class.
     */
    override fun dispatchReceiverForInnerClassConstructor(): ConeClassLikeType? {
        val dispatchReceivers = context.dispatchReceiverTypesStack
        return dispatchReceivers.getOrNull(dispatchReceivers.lastIndex - 1)
    }

    override fun callableIdForClassConstructor(): CallableId {
        val packageName = if (context.inLocalContext) {
            CallableId.PACKAGE_FQ_NAME_FOR_LOCAL
        } else {
            context.packageFqName
        }

        return if (context.className == FqName.ROOT) {
            CallableId(packageName, Name.special("<anonymous-init>"))
        } else {
            CallableId(packageName, context.className, context.className.shortName())
        }
    }


    /**** Function utils ****/
    fun <T> MutableList<T>.removeLast(): T {
        return removeAt(size - 1)
    }

    fun <T> MutableList<T>.pop(): T? {
        val result = lastOrNull()
        if (result != null) {
            removeAt(size - 1)
        }
        return result
    }

    override fun FirExpression.toReturn(
        baseSource: KtSourceElement?,
        labelName: String?,
        fromKtReturnExpression: Boolean,
    ): FirReturnExpression {
        return buildReturnExpression {
            fun FirFunctionTarget.bindToErrorFunction(message: String, kind: DiagnosticKind) {
                bind(
                    buildErrorFunction {
                        source = baseSource
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        diagnostic = ConeSimpleDiagnostic(message, kind)
                        symbol = FirErrorFunctionSymbol()
                    }
                )
            }

            source =
                if (fromKtReturnExpression) baseSource?.realElement()
                else baseSource?.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody)
            result = this@toReturn
            if (labelName == null) {
                target = context.firFunctionTargets.lastOrNull { !it.isLambda } ?: FirFunctionTarget(labelName, isLambda = false).apply {
                    bindToErrorFunction("Cannot bind unlabeled return to a function", DiagnosticKind.ReturnNotAllowed)
                }
            } else {
                for (functionTarget in context.firFunctionTargets.asReversed()) {
                    if (functionTarget.labelName == labelName) {
                        target = functionTarget
                        return@buildReturnExpression
                    }
                }
                target = FirFunctionTarget(labelName, false).apply {
                    if (context.firLabels.any { it.name == labelName }) {
                        bindToErrorFunction("Label $labelName does not target a function", DiagnosticKind.NotAFunctionLabel)
                    } else {
                        bindToErrorFunction("Cannot bind label $labelName to a function", DiagnosticKind.UnresolvedLabel)
                    }
                }
            }
        }
    }

    override fun FirLoopBuilder.prepareTarget(firLabelUser: Any): FirLoopTarget = prepareTarget(context.getLastLabel(firLabelUser))

    fun FirLoopBuilder.prepareTarget(label: FirLabel?): FirLoopTarget {
        this.label = label
        val target = FirLoopTarget(label?.name)
        context.firLoopTargets += target
        return target
    }

    override fun FirLoopBuilder.configure(target: FirLoopTarget, generateBlock: () -> FirBlock): FirLoop {
        block = generateBlock()
        val loop = build()
        val stackTopTarget = context.firLoopTargets.removeLast()
        assert(target == stackTopTarget) {
            "Loop target preparation and loop configuration mismatch"
        }
        target.bind(loop)
        return loop
    }

    override fun FirLoopJumpBuilder.bindLabel(expression: Node): FirLoopJumpBuilder {
        val labelName = expression.getLabelName()
        val lastLoopTarget = context.firLoopTargets.lastOrNull()
        val sourceElement = expression.toFirSourceElement()
        if (labelName == null) {
            target = lastLoopTarget ?: FirLoopTarget(labelName).apply {
                bind(
                    buildErrorLoop(
                        sourceElement,
                        ConeSimpleDiagnostic("Cannot bind unlabeled jump to a loop", DiagnosticKind.JumpOutsideLoop)
                    )
                )
            }
        } else {
            for (firLoopTarget in context.firLoopTargets.asReversed()) {
                if (firLoopTarget.labelName == labelName) {
                    target = firLoopTarget
                    return this
                }
            }
            target = FirLoopTarget(labelName).apply {
                bind(
                    buildErrorLoop(
                        sourceElement,
                        ConeSimpleDiagnostic(
                            "Cannot bind label $labelName to a loop",
                            lastLoopTarget?.let { DiagnosticKind.NotLoopLabel } ?: DiagnosticKind.JumpOutsideLoop
                        )
                    )
                )
            }
        }
        return this
    }

    override fun generateConstantExpressionByLiteral(expression: Node): FirExpression {
        val type = expression.elementType
        val text: String = expression.asText
        val sourceElement = expression.toFirSourceElement()

        fun reportIncorrectConstant(kind: DiagnosticKind): FirErrorExpression {
            return buildErrorExpression {
                source = sourceElement
                diagnostic = ConeSimpleDiagnostic("Incorrect constant expression: $text", kind)
            }
        }

        val tokenId = type.typeToTokenId()
        val convertedText: Any? = when (tokenId) {
            KtNodeTypes.INTEGER_CONSTANT_ID, KtNodeTypes.FLOAT_CONSTANT_ID -> when {
                text.hasIllegalUnderscore(tokenId) -> return reportIncorrectConstant(DiagnosticKind.IllegalUnderscore)
                else -> text.parseNumericLiteral(tokenId)
            }
            KtNodeTypes.BOOLEAN_CONSTANT_ID -> parseBoolean(text)
            else -> null
        }
        return when (tokenId) {
            KtNodeTypes.INTEGER_CONSTANT_ID -> {
                var diagnostic: DiagnosticKind = DiagnosticKind.IllegalConstExpression
                var number: Long?

                val kind = when {
                    convertedText == null -> {
                        number = null
                        diagnostic = DiagnosticKind.IntLiteralOutOfRange
                        ConstantValueKind.IntegerLiteral
                    }

                    convertedText !is Long -> return reportIncorrectConstant(DiagnosticKind.IllegalConstExpression)

                    hasUnsignedLongSuffix(text) -> {
                        if (text.endsWith("l")) {
                            diagnostic = DiagnosticKind.WrongLongSuffix
                            number = null
                        } else {
                            number = convertedText
                        }
                        ConstantValueKind.UnsignedLong
                    }
                    hasLongSuffix(text) -> {
                        if (text.endsWith("l")) {
                            diagnostic = DiagnosticKind.WrongLongSuffix
                            number = null
                        } else {
                            number = convertedText
                        }
                        ConstantValueKind.Long
                    }
                    hasUnsignedSuffix(text) -> {
                        number = convertedText
                        ConstantValueKind.UnsignedIntegerLiteral
                    }

                    else -> {
                        number = convertedText
                        ConstantValueKind.IntegerLiteral
                    }
                }

                if (hasLeadingZeros(text)) {
                    diagnostic = DiagnosticKind.IntLiteralWithLeadingZeros
                    number = null
                }

                buildConstOrErrorExpression(
                    sourceElement,
                    kind,
                    number,
                    "integer",
                    text,
                    diagnostic,
                )
            }
            KtNodeTypes.FLOAT_CONSTANT_ID ->
                if (convertedText is Float) {
                    buildConstOrErrorExpression(
                        sourceElement,
                        ConstantValueKind.Float,
                        convertedText,
                        "float",
                        text,
                        DiagnosticKind.FloatLiteralOutOfRange,
                    )
                } else {
                    buildConstOrErrorExpression(
                        sourceElement,
                        ConstantValueKind.Double,
                        convertedText as? Double,
                        "double",
                        text,
                        DiagnosticKind.FloatLiteralOutOfRange,
                    )
                }
            KtNodeTypes.CHARACTER_CONSTANT_ID -> {
                val characterWithDiagnostic = text.parseCharacter()
                buildConstOrErrorExpression(
                    sourceElement,
                    ConstantValueKind.Char,
                    characterWithDiagnostic.value,
                    "character",
                    text,
                    characterWithDiagnostic.getDiagnostic() ?: DiagnosticKind.IllegalConstExpression
                )
            }
            KtNodeTypes.BOOLEAN_CONSTANT_ID ->
                buildLiteralExpression(
                    sourceElement,
                    ConstantValueKind.Boolean,
                    convertedText as Boolean,
                    setType = false
                )
            KtNodeTypes.NULL_ID ->
                buildLiteralExpression(
                    sourceElement,
                    ConstantValueKind.Null,
                    null,
                    setType = false
                )
            else ->
                errorWithAttachment("Unknown literal type: $type") {
                    withSourceElementEntry("literal", expression)
                }
        }
    }

    private fun hasLeadingZeros(text: String): Boolean {
        return text.length > 1 && text[0] == '0' && text[1].let { it.isDigit() || it == '_' }
    }

    protected fun ExceptionAttachmentBuilder.withSourceElementEntry(name: String, element: Node?) {
        when (element) {
            is PsiElement -> withPsiEntry(name, element)
            else -> withEntry(name, element) { it.asText }
        }
    }

    open fun String.hasIllegalUnderscore(typeId: Int): Boolean {
        return when {
            typeId == KtNodeTypes.INTEGER_CONSTANT_ID -> hasIllegallyPositionedUnderscore(this, isFloatingPoint = false)
            else -> hasIllegallyPositionedUnderscore(this, isFloatingPoint = true)
        }
    }

    open fun String.parseNumericLiteral(typeId: Int): Number? {
        return when (typeId) {
            KtNodeTypes.INTEGER_CONSTANT_ID -> parseNumericLiteral(this, isFloatingPointLiteral = false)
            KtNodeTypes.FLOAT_CONSTANT_ID -> parseNumericLiteral(this, isFloatingPointLiteral = true)
            else -> null
        }
    }

    override fun generateIncrementOrDecrementBlock(
        // Used to get source-element or text
        wholeExpression: Node,
        operationReference: Node?,
        receiver: Node?,
        callName: Name,
        prefix: Boolean,
        convert: Node.() -> FirExpression,
    ): FirExpression {
        val unwrappedReceiver = receiver.unwrap() ?: return buildErrorExpression {
            source = wholeExpression.toFirSourceElement()
            diagnostic = ConeSyntaxDiagnostic("Inc/dec without operand")
        }

        if (unwrappedReceiver.toTokenId() == KtNodeTypes.ARRAY_ACCESS_EXPRESSION_ID) {
            return generateIncrementOrDecrementBlockForArrayAccess(
                wholeExpression,
                operationReference,
                unwrappedReceiver,
                callName,
                prefix,
                convert
            )
        }

        return buildIncrementDecrementExpression {
            val baseSource = wholeExpression.toFirSourceElement()
            source = baseSource
            operationSource = operationReference?.toFirSourceElement()
            operationName = callName
            isPrefix = prefix
            expression = unwrappedReceiver.convert()
        }.pullUpSafeCallIfNecessary(
            obtainReceiver = FirIncrementDecrementExpression::expression,
            replaceReceiver = FirIncrementDecrementExpression::replaceExpression
        )
    }

    override fun FirQualifiedAccessExpression.pullUpSafeCallIfNecessary(): FirExpression =
        pullUpSafeCallIfNecessary(
            FirQualifiedAccessExpression::explicitReceiver,
            FirQualifiedAccessExpression::replaceExplicitReceiver
        )

    // Turns a?.b.f(...) to a?.{ b.f(...) ) -- for any qualified access `.f(...)`
    // Other patterns remain unchanged
    private fun <F : FirExpression> F.pullUpSafeCallIfNecessary(
        obtainReceiver: F.() -> FirExpression?,
        replaceReceiver: F.(FirExpression) -> Unit,
    ): FirExpression {
        val safeCall = obtainReceiver() as? FirSafeCallExpression ?: return this
        val safeCallSelector = safeCall.selector as? FirExpression ?: return this

        // (a?.b).f and `(a?.b)[3]` should be left as is
        if (safeCall.isChildInParentheses()) return this

        replaceReceiver(safeCallSelector)
        safeCall.replaceSelector(this)

        return safeCall
    }

    private fun FirStatement.isChildInParentheses(): Boolean {
        val sourceElement = source ?: error("Nullable source")
        return sourceElement.isChildInParentheses()
    }

    open fun KtSourceElement.isChildInParentheses(): Boolean =
        treeStructure.getParent(lighterASTNode)?.tokenType == org.jetbrains.kotlin.KtNodeTypes.PARENTHESIZED

    /**
     * See [UNWRAPPABLE_TOKEN_TYPES][org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES]
     */
    private fun Node?.unwrap(): Node? {
        // NOTE: By removing surrounding parentheses and labels, FirLabels will NOT be created for those labels.
        // This should be fine since the label is meaningless and unusable for a ++/-- argument or assignment LHS.
        var unwrapped = this
        while (true) {
            val tokenId = unwrapped?.toTokenId()
            unwrapped = when (tokenId) {
                null -> return unwrapped
                KtNodeTypes.PARENTHESIZED_ID -> unwrapped.getExpressionInParentheses()
                KtNodeTypes.LABELED_EXPRESSION_ID -> unwrapped.getLabeledExpression()
                KtNodeTypes.ANNOTATED_EXPRESSION_ID -> unwrapped.getAnnotatedExpression()
                else -> return unwrapped
            }
        }
    }

    /**
     * given:
     * a[b, c]++
     *
     * result:
     * {
     *     val <array> = a
     *     val <index0> = b
     *     val <index1> = c
     *     val <unary> = <array>.get(<index0>, <index1>)
     *     <array>.set(<index0>, <index1>, <unary>.inc())
     *     ^<unary>
     * }
     *
     * given:
     * ++a[b, c]
     *
     * result:
     * {
     *     val <array> = a
     *     val <index0> = b
     *     val <index1> = c
     *     <array>.set(b, c, <array>.get(<index0>, <index1>).inc())
     *     ^<array>.get(<index0>, <index1>)
     * }
     *
     */
    private fun generateIncrementOrDecrementBlockForArrayAccess(
        wholeExpression: Node,
        operationReference: Node?,
        receiver: Node,
        callName: Name,
        prefix: Boolean,
        convert: Node.() -> FirExpression,
    ): FirExpression {
        val array = receiver.arrayExpression
        val sourceKind = sourceKindForIncOrDec(callName, prefix)
        val receiverSourceElement = receiver.toFirSourceElement()
        return buildBlockPossiblyUnderSafeCall(
            array, convert,
            // For (a?.b[3])++ and (a?.b)[3]++ we should not pull `++` inside safe call
            isChildInParentheses = receiverSourceElement.isChildInParentheses() ||
                    array?.toFirSourceElement()?.isChildInParentheses() == true,
            sourceElementForError = receiverSourceElement,
        ) { arrayReceiver ->
            val baseSource = wholeExpression.toFirSourceElement()
            val desugaredSource = baseSource.fakeElement(sourceKind)
            source = desugaredSource

            val indices = receiver.indexExpressions
            requireNotNull(indices) { "No indices in ${wholeExpression.asText}" }

            val arrayVariable = generateTemporaryVariable(
                baseModuleData,
                array?.toFirSourceElement(KtFakeSourceElementKind.ArrayAccessNameReference),
                name = SpecialNames.ARRAY,
                initializer = arrayReceiver,
            ).also { statements += it }

            val indexVariables = indices.mapIndexed { i, index ->
                generateTemporaryVariable(
                    baseModuleData,
                    index.toFirSourceElement(KtFakeSourceElementKind.ArrayIndexExpressionReference),
                    name = SpecialNames.subscribeOperatorIndex(i),
                    index.convert()
                ).also { statements += it }
            }

            fun buildGetCall(sourceKind: KtFakeSourceElementKind) =
                buildFunctionCall {
                    val fakeSource = receiver.toFirSourceElement(sourceKind)
                    source = fakeSource
                    calleeReference = buildSimpleNamedReference {
                        source = fakeSource
                        name = OperatorNameConventions.GET
                    }
                    explicitReceiver = generateResolvedAccessExpression(arrayVariable.source, arrayVariable)
                    argumentList = buildArgumentList {
                        for (indexVar in indexVariables) {
                            arguments += generateResolvedAccessExpression(indexVar.source, indexVar)
                        }
                    }
                    origin = FirFunctionCallOrigin.Operator
                }

            fun buildSetCall(argumentExpression: FirExpression, sourceElementKind: KtFakeSourceElementKind) = buildFunctionCall {
                source = desugaredSource
                calleeReference = buildSimpleNamedReference {
                    source = receiver.toFirSourceElement(sourceElementKind)
                    name = OperatorNameConventions.SET
                }
                explicitReceiver = generateResolvedAccessExpression(arrayVariable.source, variable = arrayVariable)
                argumentList = buildArgumentList {
                    for (indexVar in indexVariables) {
                        arguments += generateResolvedAccessExpression(indexVar.source, indexVar)
                    }
                    arguments += argumentExpression
                }
                origin = FirFunctionCallOrigin.Operator
            }

            fun buildIncDecCall(kind: KtFakeSourceElementKind, receiver: FirExpression) = buildFunctionCall {
                source = desugaredSource
                calleeReference = buildSimpleNamedReference {
                    source = operationReference?.toFirSourceElement(kind)
                    name = callName
                }
                explicitReceiver = receiver
                origin = FirFunctionCallOrigin.Operator
            }

            if (prefix) {
                statements += buildSetCall(
                    buildIncDecCall(
                        sourceKind,
                        buildGetCall(sourceKind),
                    ),
                    sourceKind
                )
                statements += buildGetCall(sourceKind.forSecondGetReference)
            } else {
                val unaryVariableSource = baseSource.fakeElement(sourceKind.forUnaryVariable)
                val initialValueVar = generateTemporaryVariable(
                    baseModuleData,
                    unaryVariableSource,
                    SpecialNames.UNARY,
                    buildGetCall(sourceKind)
                )

                statements += initialValueVar

                statements += buildSetCall(
                    buildIncDecCall(
                        sourceKind,
                        generateResolvedAccessExpression(null, initialValueVar)
                    ),
                    sourceKind
                )
                statements += generateResolvedAccessExpression(null, initialValueVar)
            }
        }
    }

    private fun buildBlockPossiblyUnderSafeCall(
        receiver: Node?,
        convert: Node.() -> FirExpression,
        isChildInParentheses: Boolean,
        sourceElementForError: KtSourceElement,
        init: FirBlockBuilder.(receiver: FirExpression) -> Unit = {},
    ): FirExpression {
        val receiverFir = receiver?.convert() ?: buildErrorExpression {
            source = sourceElementForError
            diagnostic = ConeSyntaxDiagnostic("No receiver expression")
        }

        return buildPossiblyUnderSafeCall(receiverFir, isChildInParentheses, sourceElementForError) { actualReceiver ->
            buildBlock { init(actualReceiver) }
        } as FirExpression
    }

    // if `receiver` is a safe call a?.f(...), insert a block under safe call
    // a?.{ val receiver = $subj$.f() ... } where `...` is generated by `buildSelector(FIR<$subj$.f()>)`
    //
    // Otherwise just returns buildSelector(FIR<receiver>)
    private fun buildPossiblyUnderSafeCall(
        receiver: FirExpression,
        // In most cases, the parameter is equal to `receiver.source.isChildInParentheses()`,
        // besides the case with `generateIncrementOrDecrementBlockForArrayAccess`
        isReceiverIsWrappedWithParentheses: Boolean,
        sourceElementForErrorIfSafeCallSelectorIsNotExpression: KtSourceElement?,
        buildSelector: (receiver: FirExpression) -> FirStatement,
    ): FirStatement {
        // For (a?.b*).f() we would not pull `f` under a safe call
        if (receiver is FirSafeCallExpression && !isReceiverIsWrappedWithParentheses) {
            receiver.replaceSelector(
                buildSelector(
                    receiver.selector as? FirExpression ?: buildErrorExpression {
                        source = sourceElementForErrorIfSafeCallSelectorIsNotExpression
                        diagnostic = ConeSyntaxDiagnostic("Safe call selector expected to be an expression here")
                    }
                )
            )

            return receiver
        }

        return buildSelector(receiver)
    }

    @OptIn(FirContractViolation::class)
    override fun Node?.generateAssignment(
        baseSource: KtSourceElement,
        arrayAccessSource: KtSourceElement?,
        rhsExpression: FirExpression,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        // Effectively `value = rhs?.convert()`, but at generateIndexedAccessAugmentedAssignment we need to recreate FIR for rhs
        // since there should be different nodes for desugaring as `.set(.., get().plus($rhs1))` and `.get(...).plusAssign($rhs2)`
        // Once KT-50861 is fixed, those two parameters shall be eliminated
        rhsAST: Node?,
        isLhsParenthesized: Boolean,
        convert: Node.() -> FirExpression,
    ): FirStatement {
        val unwrappedLhs = this.unwrap() ?: return buildErrorExpression {
            source = baseSource
            diagnostic = ConeSyntaxDiagnostic("Inc/dec without operand")
        }

        if (unwrappedLhs.toTokenId() == KtNodeTypes.ARRAY_ACCESS_EXPRESSION_ID) {
            if (operation == FirOperation.ASSIGN) {
                context.arraySetArgument[unwrappedLhs] = rhsExpression
            }
            return buildBlock {
                if (operation == FirOperation.ASSIGN) {
                    val result = unwrappedLhs.convert()
                    result.replaceAnnotations(result.annotations.smartPlus(annotations))
                    source = result.source?.fakeElement(KtFakeSourceElementKind.IndexedAssignmentCoercionBlock)
                    statements += (result as? FirQualifiedAccessExpression)?.pullUpSafeCallIfNecessary() ?: result
                } else {
                    val receiver = unwrappedLhs.convert()
                    val result = buildPossiblyUnderSafeCall(
                        receiver,
                        // For (a?.b[3]) += 1 we don't want to pull `+=` under a safe call
                        isReceiverIsWrappedWithParentheses = unwrappedLhs.toFirSourceElement().isChildInParentheses(),
                        sourceElementForErrorIfSafeCallSelectorIsNotExpression = receiver.source,
                    ) { actualReceiver ->
                        generateIndexedAccessAugmentedAssignment(
                            actualReceiver, baseSource, arrayAccessSource, operation, annotations, rhsAST, convert, isLhsParenthesized,
                        )
                    }
                    source = result.source?.fakeElement(KtFakeSourceElementKind.IndexedAssignmentCoercionBlock)
                    statements += result
                }
                statements += buildUnitExpression {
                    source = baseSource.fakeElement(KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion)
                }
            }
        }

        if (operation in FirOperation.ASSIGNMENTS && operation != FirOperation.ASSIGN) {
            val lhsReceiver = this@generateAssignment?.convert()
            if (lhsReceiver is FirQualifiedAccessExpression) {
                @OptIn(FirImplementationDetail::class)
                lhsReceiver.replaceSource(lhsReceiver.source?.fakeElement(operation.toAugmentedAssignSourceKind()))
            }

            val receiverToUse =
                lhsReceiver ?: buildErrorExpression {
                    source = baseSource
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource.psi?.text}", DiagnosticKind.ExpressionExpected
                    )
                }

            val prohibitSetCallsForParenthesizedLhs = this@AbstractRawFirBuilder.baseSession.languageVersionSettings.supportsFeature(
                LanguageFeature.ForbidParenthesizedLhsInAssignments
            )

            return buildPossiblyUnderSafeCall(
                receiverToUse,
                // For (a?.b) += 1 we don't want to pull `+=` under a safe call
                isReceiverIsWrappedWithParentheses = isLhsParenthesized,
                sourceElementForErrorIfSafeCallSelectorIsNotExpression = null
            ) { actualReceiver ->
                // Disable `set` resolution for `(c?.p) += ...` where `p` has an extension operator `plus()`.
                if (isLhsParenthesized && prohibitSetCallsForParenthesizedLhs) {
                    generateAssignmentOperatorCall(operation, baseSource, receiverToUse, rhsExpression, annotations)
                } else {
                    buildAugmentedAssignment {
                        source = baseSource
                        this.operation = operation
                        leftArgument = actualReceiver
                        rightArgument = rhsExpression
                        this.annotations += annotations
                    }
                }
            }
        }
        require(operation == FirOperation.ASSIGN)

        if (this?.toTokenId() == KtNodeTypes.SAFE_ACCESS_EXPRESSION_ID) {
            val safeCallNonAssignment = convert() as? FirSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, rhsExpression, annotations)
            }
        }

        val assignmentLValue = unwrappedLhs.convert()
        return buildVariableAssignment {
            source = baseSource
            lValue = if (baseSource.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
                buildDesugaredAssignmentValueReferenceExpression {
                    expressionRef = FirExpressionRef<FirExpression>().apply { bind(assignmentLValue) }
                    source =
                        assignmentLValue.source?.fakeElement(baseSource.kind as KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
                            ?: baseSource.fakeElement(KtFakeSourceElementKind.DesugaredAssignmentLValueSourceIsNull)
                }
            } else {
                assignmentLValue
            }
            rValue = rhsExpression
            this.annotations += annotations
        }
    }

    // gets a?.{ $subj.x } and turns it to a?.{ $subj.x = v }
    private fun putAssignmentToSafeCall(
        safeCallNonAssignment: FirSafeCallExpression,
        baseSource: KtSourceElement?,
        rhsExpression: FirExpression,
        annotations: List<FirAnnotation>,
    ): FirSafeCallExpression {
        val nestedAccess = safeCallNonAssignment.selector as FirQualifiedAccessExpression

        val assignment = buildVariableAssignment {
            source = baseSource
            lValue = nestedAccess
            rValue = rhsExpression
            this.annotations += annotations
        }

        safeCallNonAssignment.replaceSelector(
            assignment
        )

        return safeCallNonAssignment
    }

    private fun generateIndexedAccessAugmentedAssignment(
        receiver: FirExpression, // a.get(x,y)
        baseSource: KtSourceElement,
        arrayAccessSource: KtSourceElement?,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        rhs: Node?,
        convert: Node.() -> FirExpression,
        isLhsParenthesized: Boolean,
    ): FirStatement {
        val prohibitSetCallsForParenthesizedLhs = this@AbstractRawFirBuilder.baseSession.languageVersionSettings.supportsFeature(
            LanguageFeature.ForbidParenthesizedLhsInAssignments
        )

        // For case of LHS is a parenthesized safe call, like (a?.b[3]) += 1
        // Here, we explicitly declare that it can't be desugared as `a?.{ b[3] = b[3] + 1 }` or
        // as some other sort of `plus` + set, thus we leave only `plusAssign` form.
        // Also, disable `set` resolution for `(a[0]) += ...` where `a: Array<A>`.
        if (receiver is FirSafeCallExpression || isLhsParenthesized && prohibitSetCallsForParenthesizedLhs) {
            val argument = rhs?.convert() ?: buildErrorExpression(
                baseSource,
                ConeSyntaxDiagnostic("No value for array set")
            )
            return generateAssignmentOperatorCall(operation, baseSource, receiver, argument, annotations)
        }

        require(receiver is FirFunctionCall) {
            "Array access should be desugared to a function call, but $receiver is found"
        }

        return buildIndexedAccessAugmentedAssignment {
            source = baseSource
            this.operation = operation
            this.lhsGetCall = receiver
            this.rhs = rhs?.convert() ?: buildErrorExpression(
                baseSource,
                ConeSyntaxDiagnostic("No value for array set")
            )
            this.arrayAccessSource = arrayAccessSource
            this.annotations += annotations
        }
    }

    private fun generateAssignmentOperatorCall(
        operation: FirOperation,
        source: KtSourceElement?,
        receiver: FirExpression?,
        rhsExpression: FirExpression,
        annotations: List<FirAnnotation>,
    ): FirFunctionCall {
        return buildFunctionCall {
            this.source = source
            explicitReceiver = receiver
            argumentList = buildUnaryArgumentList(rhsExpression)

            calleeReference = buildSimpleNamedReference {
                this.source = source
                this.name = FirOperationNameConventions.ASSIGNMENTS.getValue(operation)
            }
            origin = FirFunctionCallOrigin.Operator
            this.annotations.addAll(annotations)
        }
    }

    override fun generateDataClassMembers(
        source: Node,
        classBuilder: FirRegularClassBuilder,
        firPrimaryConstructor: FirConstructor,
        zippedParameters: List<Pair<Node, FirProperty>>,
        packageFqName: FqName,
        classFqName: FqName,
        addValueParameterAnnotations: FirValueParameterBuilder.(Node) -> Unit
    ) {
        DataClassMembersGenerator(
            source,
            classBuilder,
            firPrimaryConstructor,
            zippedParameters,
            packageFqName,
            classFqName,
            addValueParameterAnnotations
        ).generate()
    }

    /**
     * Generates the synthetic members of a data class.
     *
     * The fake source elements of the generated members should be distinct per the contract of [KtSourceElement]. Hence, the generator must
     * ensure that each pair of `(realSource, fakeElementKind)` is distinct.
     */
    inner class DataClassMembersGenerator(
        private val source: Node,
        private val classBuilder: FirRegularClassBuilder,
        private val firPrimaryConstructor: FirConstructor,
        private val zippedParameters: List<Pair<Node, FirProperty>>,
        private val packageFqName: FqName,
        private val classFqName: FqName,
        private val addValueParameterAnnotations: FirValueParameterBuilder.(Node) -> Unit,
    ) {
        fun generate() {
            if (classBuilder.classKind != ClassKind.OBJECT) {
                generateComponentFunctions()
                generateCopyFunction()
            }
            // Refer to (IR utils or FIR backend) DataClassMembersGenerator for generating equals, hashCode, and toString
        }

        private fun generateComponentFunctions() {
            var componentIndex = 1
            for ([sourceNode, firProperty] in zippedParameters) {
                if (!firProperty.isVal && !firProperty.isVar) continue
                val name = Name.identifier("component$componentIndex")
                componentIndex++
                val componentSource =
                    sourceNode.toFirSourceElement(KtFakeSourceElementKind.DataClassGeneratedMembers.ComponentFunction)

                val componentFunction = buildNamedFunction {
                    source = componentSource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Synthetic.DataClassMember

                    // The return type reference has a different real source than the component function, so we can reuse
                    // `ComponentFunction`.
                    returnTypeRef = firProperty.returnTypeRef
                        .copyWithNewSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers.ComponentFunction)

                    this.name = name
                    status = FirDeclarationStatusImpl(firProperty.visibility, Modality.FINAL).apply {
                        isOperator = true
                    }
                    isLocal = firPrimaryConstructor.isLocal
                    symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))
                    dispatchReceiverType = currentDispatchReceiverType()
                    // Refer to FIR backend ClassMemberGenerator for body generation.
                }.also {
                    firProperty.componentFunctionSymbol = it.symbol
                }
                classBuilder.addDeclaration(componentFunction)
            }
        }

        private fun generateCopyFunction() {
            classBuilder.addDeclaration(
                classBuilder.createDataClassCopyFunction(
                    ClassId(packageFqName, classFqName, isLocal = false),
                    source,
                    currentDispatchReceiverType(),
                    zippedParameters,
                    isFromLibrary = false,
                    firPrimaryConstructor,
                    { src, kind -> src.toFirSourceElement(kind) },
                    addValueParameterAnnotations,
                    { it.isVararg },
                )
            )
        }
    }

    override fun FirClassLikeDeclaration.initContainingClassForLocalAttr() {
        if (isLocal) {
            val currentDispatchReceiverType = currentDispatchReceiverType()
            if (currentDispatchReceiverType != null) {
                containingClassForLocalAttr = currentDispatchReceiverType.lookupTag
            }
        }
    }

    override fun FirRegularClass.initContainingScriptOrReplAttr() {
        context.containingScriptSymbol?.let { script ->
            containingScriptSymbolAttr = script
        }
        context.containingReplSymbol?.let { repl ->
            containingReplSymbolAttr = repl
        }
    }

    override fun FirCallableDeclaration.initContainingClassAttr() {
        initContainingClassAttr(context)
    }

    protected fun getForbiddenLabelKind(rawName: String, isMultipleLabel: Boolean): ForbiddenLabelKind? = when {
        rawName.isUnderscore -> ForbiddenLabelKind.UNDERSCORE_IS_RESERVED
        isMultipleLabel -> ForbiddenLabelKind.MULTIPLE_LABEL
        else -> null
    }

    enum class ForbiddenLabelKind {
        UNDERSCORE_IS_RESERVED, MULTIPLE_LABEL
    }

    protected open fun isReplSnippet(script: Node, sourceFile: KtSourceFile): Boolean {
        val scriptSource = script.toFirSourceElement()
        return baseSession.extensionService.replSnippetConfigurators.any {
            it.isReplSnippetsSource(sourceFile, scriptSource)
        }
    }

    /**
     * Converts the [declaration] to a [FirScript] or [FirReplSnippet] depending on the [sourceFile].
     *
     * If [fileBuilder] is provided, it will be used to configure the file containing the script or snippet.
     */
    override fun convertScriptOrSnippets(declaration: Node, sourceFile: KtSourceFile, fileBuilder: FirFileBuilder?): FirDeclaration {
        val scriptSource = declaration.toFirSourceElement()

        return if (isReplSnippet(declaration, sourceFile)) {
            val repSnippetConfigurator = baseSession.extensionService.replSnippetConfigurators.filter {
                it.isReplSnippetsSource(sourceFile, scriptSource)
            }.let { snippetConfiguratorExtensions ->
                requireWithAttachment(
                    snippetConfiguratorExtensions.size <= 1,
                    message = { "More than one REPL snippet configurator is found for the file" },
                ) {
                    withEntry("fileName", sourceFile.name)
                    withEntry("configurators", snippetConfiguratorExtensions.joinToString { "${it::class.java.name}" })
                }

                snippetConfiguratorExtensions.firstOrNull()
            }

            convertReplSnippet(
                script = declaration,
                scriptSource = scriptSource,
                fileName = sourceFile.name,
                snippetSetup = {
                    if (repSnippetConfigurator != null) {
                        with(repSnippetConfigurator) {
                            fileBuilder?.let { configureContainingFile(it) }
                            configure(sourceFile, context)
                        }
                    }
                },
                functionBodySetup = {
                    if (repSnippetConfigurator != null) {
                        with(repSnippetConfigurator) {
                            configureEvalBody(sourceFile, scriptSource, context)
                        }
                    }
                },
                statementsSetup = {
                    if (repSnippetConfigurator != null) {
                        with(repSnippetConfigurator) {
                            configure(sourceFile, scriptSource, context)
                        }
                    }
                },
            )
        } else {
            val scriptConfigurator = baseSession.extensionService.scriptConfigurators.firstOrNull {
                it.accepts(sourceFile, scriptSource)
            }

            convertScript(declaration, scriptSource, sourceFile.name) {
                if (scriptConfigurator != null) {
                    with(scriptConfigurator) {
                        fileBuilder?.let { configureContainingFile(it) }
                        configure(sourceFile, context)
                    }
                }
            }
        }
    }

    protected abstract fun convertScript(
        script: Node,
        scriptSource: KtSourceElement,
        fileName: String,
        setup: FirScriptBuilder.() -> Unit,
    ): FirScript

    protected abstract fun convertReplSnippet(
        script: Node,
        scriptSource: KtSourceElement,
        fileName: String,
        snippetSetup: FirReplSnippetBuilder.() -> Unit,
        functionBodySetup: FirBlockBuilder.() -> Unit,
        statementsSetup: MutableList<FirElement>.() -> Unit,
    ): FirReplSnippet
}

fun <TBase, TSource : TBase, TParameter : TBase> FirRegularClassBuilder.createDataClassCopyFunction(
    classId: ClassId,
    sourceElement: TSource,
    dispatchReceiver: ConeClassLikeType?,
    zippedParameters: List<Pair<TParameter, FirProperty>>,
    isFromLibrary: Boolean,
    firConstructor: FirConstructor,
    toFirSource: (TBase, KtFakeSourceElementKind) -> KtSourceElement,
    addValueParameterAnnotations: FirValueParameterBuilder.(TParameter) -> Unit,
    isVararg: (TParameter) -> Boolean,
): FirNamedFunction {
    fun generateComponentAccess(
        parameterSource: KtSourceElement?,
        firProperty: FirProperty,
        classTypeRefWithCorrectSourceKind: FirTypeRef,
        firPropertyReturnTypeRefWithCorrectSourceKind: FirTypeRef,
    ) =
        buildPropertyAccessExpression {
            this.source = parameterSource
            coneTypeOrNull = firPropertyReturnTypeRefWithCorrectSourceKind.coneTypeOrNull
            this.dispatchReceiver = buildThisReceiverExpression {
                this.source = parameterSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = this@createDataClassCopyFunction.symbol
                }
                coneTypeOrNull = classTypeRefWithCorrectSourceKind.coneTypeOrNull
            }
            calleeReference = buildResolvedNamedReference {
                this.source = parameterSource
                this.name = firProperty.name
                resolvedSymbol = firProperty.symbol
            }
        }

    val declarationOrigin = if (isFromLibrary) FirDeclarationOrigin.Library else FirDeclarationOrigin.Synthetic.DataClassMember

    return buildNamedFunction {
        val copySourceElement = toFirSource(sourceElement, KtFakeSourceElementKind.DataClassGeneratedMembers.CopyFunction)

        // The return type reference has a different real source than the copy function, so we can reuse `CopyFunction`.
        val classTypeRef = firConstructor.returnTypeRef
            .copyWithNewSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers.CopyFunction)

        this.source = copySourceElement
        moduleData = this@createDataClassCopyFunction.moduleData
        origin = declarationOrigin
        returnTypeRef = classTypeRef
        name = StandardNames.DATA_CLASS_COPY
        symbol = FirNamedFunctionSymbol(CallableId(classId.packageFqName, classId.relativeClassName, StandardNames.DATA_CLASS_COPY))
        dispatchReceiverType = dispatchReceiver
        resolvePhase = this@createDataClassCopyFunction.resolvePhase
        // We need to resolve annotations on the data class. It's not possible to do it in the RAW_FIR phase.
        // We will resolve the visibility later in the STATUS phase
        status = if (isFromLibrary) {
            FirResolvedDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL, EffectiveVisibility.Unknown)
        } else {
            FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
        }
        isLocal = firConstructor.isLocal

        for ([ktParameter, firProperty] in zippedParameters) {
            val propertyName = firProperty.name
            val parameterSource = toFirSource(ktParameter, KtFakeSourceElementKind.DataClassGeneratedMembers.CopyFunction.Parameter)

            // The return type reference has a different real source than the parameter, so we can reuse `CopyFunction.Parameter`.
            val propertyReturnTypeRef = firProperty.returnTypeRef
                .copyWithNewSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers.CopyFunction.Parameter)

            valueParameters += buildValueParameter {
                resolvePhase = this@createDataClassCopyFunction.resolvePhase
                source = parameterSource
                containingDeclarationSymbol = this@buildNamedFunction.symbol
                moduleData = this@createDataClassCopyFunction.moduleData
                origin = declarationOrigin
                returnTypeRef = propertyReturnTypeRef
                name = propertyName
                symbol = FirValueParameterSymbol()
                defaultValue = generateComponentAccess(parameterSource, firProperty, classTypeRef, propertyReturnTypeRef)
                isCrossinline = false
                isNoinline = false
                this.isVararg = isVararg(ktParameter)
                addValueParameterAnnotations(ktParameter)
                for (annotation in annotations) {
                    annotation.replaceUseSiteTarget(null)
                }
            }
        }
        // Refer to FIR backend ClassMemberGenerator for body generation.
    }
}

/**
 * Not the same as [filterStandalonePropertyRelevantAnnotations], because on
 * primary constructor value parameters annotations should go to the
 * [FirValueParameter] first.
 */
fun List<FirAnnotationCall>.filterConstructorPropertyRelevantAnnotations(isVar: Boolean): List<FirAnnotationCall> = filter {
    it.useSiteTarget == null || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY || it.useSiteTarget == AnnotationUseSiteTarget.ALL
            || !isVar && (it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER)
}

fun List<FirAnnotationCall>.filterStandalonePropertyRelevantAnnotations(isVar: Boolean): List<FirAnnotationCall> = filter {
    it.useSiteTarget != AnnotationUseSiteTarget.FIELD && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_GETTER &&
            (!isVar || it.useSiteTarget != AnnotationUseSiteTarget.SETTER_PARAMETER && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_SETTER)
}
