/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.parsing.*
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

//T can be either PsiElement, or LighterASTNode
abstract class AbstractRawFirBuilder<T>(val baseSession: FirSession, val context: Context<T> = Context()) {
    val baseModuleData: FirModuleData = baseSession.moduleData

    abstract fun T.toFirSourceElement(kind: KtFakeSourceElementKind? = null): KtSourceElement

    protected val implicitUnitType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.unitType
    protected val implicitAnyType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.anyType
    protected val implicitEnumType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.enumType
    protected val implicitAnnotationType: FirImplicitBuiltinTypeRef = baseSession.builtinTypes.annotationType

    abstract val T.elementType: IElementType
    abstract val T.asText: String
    abstract val T.unescapedValue: String
    abstract fun T.getReferencedNameAsName(): Name
    abstract fun T.getLabelName(): String?
    abstract fun T.getExpressionInParentheses(): T?
    abstract fun T.getAnnotatedExpression(): T?
    abstract fun T.getLabeledExpression(): T?
    abstract fun T.getChildNodeByType(type: IElementType): T?
    abstract val T?.receiverExpression: T?
    abstract val T?.selectorExpression: T?
    abstract val T?.arrayExpression: T?
    abstract val T?.indexExpressions: List<T>?
    abstract val T.isVararg: Boolean

    /**** Class name utils ****/
    inline fun <T> withChildClassName(
        name: Name,
        isExpect: Boolean,
        forceLocalContext: Boolean = false,
        l: () -> T,
    ): T = when {
        forceLocalContext -> withForcedLocalContext {
            withChildClassNameRegardlessLocalContext(name, isExpect, l)
        }
        else -> {
            withChildClassNameRegardlessLocalContext(name, isExpect, l)
        }
    }

    inline fun <T> withChildClassNameRegardlessLocalContext(
        name: Name,
        isExpect: Boolean,
        l: () -> T,
    ): T {
        context.className = context.className.child(name)
        val previousIsExpect = context.containerIsExpect
        context.containerIsExpect = previousIsExpect || isExpect
        val dispatchReceiversNumber = context.dispatchReceiverTypesStack.size
        return try {
            l()
        } finally {
            require(context.dispatchReceiverTypesStack.size <= dispatchReceiversNumber + 1) {
                "Wrong number of ${context.dispatchReceiverTypesStack.size}"
            }

            if (context.dispatchReceiverTypesStack.size > dispatchReceiversNumber) {
                context.dispatchReceiverTypesStack.removeAt(context.dispatchReceiverTypesStack.lastIndex)
            }

            context.className = context.className.parent()
            context.containerIsExpect = previousIsExpect
        }
    }

    inline fun <R> withForcedLocalContext(block: () -> R): R {
        val oldForcedLocalContext = context.inLocalContext
        context.inLocalContext = true
        val oldClassNameBeforeLocalContext = context.classNameBeforeLocalContext
        if (!oldForcedLocalContext) {
            context.classNameBeforeLocalContext = context.className
        }
        val oldClassName = context.className
        context.className = FqName.ROOT
        return try {
            block()
        } finally {
            context.classNameBeforeLocalContext = oldClassNameBeforeLocalContext
            context.inLocalContext = oldForcedLocalContext
            context.className = oldClassName
        }
    }

    fun registerSelfType(selfType: FirResolvedTypeRef) {
        context.dispatchReceiverTypesStack.add(selfType.coneType as ConeClassLikeType)
    }

    protected inline fun <T> withCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement? = null,
        currentFirTypeParameters: List<FirTypeParameterRef>,
        block: () -> T,
    ): T {
        addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        return try {
            block()
        } finally {
            context.popFirTypeParameters()
        }
    }

    /**
     * @param isLocal if true [symbol] will be ignored
     *
     * @see Context.containerSymbol
     * @see Context.pushContainerSymbol
     * @see Context.popContainerSymbol
     */
    inline fun <T> withContainerSymbol(
        symbol: FirBasedSymbol<*>,
        isLocal: Boolean = false,
        block: () -> T,
    ): T {
        if (!isLocal) {
            context.pushContainerSymbol(symbol)
        }

        return try {
            block()
        } finally {
            if (!isLocal) {
                context.popContainerSymbol(symbol)
            }
        }
    }

    inline fun <T> withContainerScriptSymbol(
        symbol: FirScriptSymbol,
        block: () -> T,
    ): T {
        require(context.containingScriptSymbol == null) { "Nested scripts are not supported" }
        context.containingScriptSymbol = symbol
        context.pushContainerSymbol(symbol)
        return try {
            block()
        } finally {
            context.popContainerSymbol(symbol)
            context.containingScriptSymbol = null
        }
    }

    protected open fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>,
    ) {
        context.pushFirTypeParameters(status, currentFirTypeParameters)
    }

    fun callableIdForName(name: Name): CallableId =
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
     * @return second from the end dispatch receiver. For the inner class constructor it would be the outer class.
     */
    protected fun dispatchReceiverForInnerClassConstructor(): ConeClassLikeType? {
        val dispatchReceivers = context.dispatchReceiverTypesStack
        return dispatchReceivers.getOrNull(dispatchReceivers.lastIndex - 1)
    }

    fun callableIdForClassConstructor(): CallableId {
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

    fun FirExpression.toReturn(
        baseSource: KtSourceElement? = source,
        labelName: String? = null,
        fromKtReturnExpression: Boolean = false,
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

    fun T?.toDelegatedSelfType(firClass: FirRegularClassBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)

    fun T?.toDelegatedSelfType(firObject: FirAnonymousObjectBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firObject.typeParameters, firObject.symbol)

    protected fun T?.toDelegatedSelfType(typeParameters: List<FirTypeParameterRef>, symbol: FirClassLikeSymbol<*>): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = this@toDelegatedSelfType?.toFirSourceElement(KtFakeSourceElementKind.ClassSelfTypeRef)
            coneType = ConeClassLikeTypeImpl(
                symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }
    }

    fun constructorTypeParametersFromConstructedClass(ownerTypeParameters: List<FirTypeParameterRef>): List<FirTypeParameterRef> {
        return ownerTypeParameters.mapNotNull {
            val declaredTypeParameter = (it as? FirTypeParameter) ?: return@mapNotNull null
            buildConstructedClassTypeParameterRef {
                source = declaredTypeParameter.symbol.source?.fakeElement(KtFakeSourceElementKind.ConstructorTypeParameter)
                symbol = declaredTypeParameter.symbol
            }
        }
    }

    fun createErrorConstructorBuilder(diagnostic: ConeDiagnostic): FirErrorPrimaryConstructorBuilder =
        FirErrorPrimaryConstructorBuilder().apply { this.diagnostic = diagnostic }

    fun FirLoopBuilder.prepareTarget(firLabelUser: Any): FirLoopTarget = prepareTarget(context.getLastLabel(firLabelUser))

    fun FirLoopBuilder.prepareTarget(label: FirLabel?): FirLoopTarget {
        this.label = label
        val target = FirLoopTarget(label?.name)
        context.firLoopTargets += target
        return target
    }

    fun FirLoopBuilder.configure(target: FirLoopTarget, generateBlock: () -> FirBlock): FirLoop {
        block = generateBlock()
        val loop = build()
        val stackTopTarget = context.firLoopTargets.removeLast()
        assert(target == stackTopTarget) {
            "Loop target preparation and loop configuration mismatch"
        }
        target.bind(loop)
        return loop
    }

    fun FirLoopJumpBuilder.bindLabel(expression: T): FirLoopJumpBuilder {
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

    fun generateConstantExpressionByLiteral(expression: T): FirExpression {
        val type = expression.elementType
        val text: String = expression.asText
        val sourceElement = expression.toFirSourceElement()

        fun reportIncorrectConstant(kind: DiagnosticKind): FirErrorExpression {
            return buildErrorExpression {
                source = sourceElement
                diagnostic = ConeSimpleDiagnostic("Incorrect constant expression: $text", kind)
            }
        }

        val convertedText: Any? = when (type) {
            INTEGER_CONSTANT, FLOAT_CONSTANT -> when {
                hasIllegalUnderscore(text, type) -> return reportIncorrectConstant(DiagnosticKind.IllegalUnderscore)
                else -> parseNumericLiteral(text, type)
            }
            BOOLEAN_CONSTANT -> parseBoolean(text)
            else -> null
        }
        return when (type) {
            INTEGER_CONSTANT -> {
                var diagnostic: DiagnosticKind = DiagnosticKind.IllegalConstExpression
                val number: Long?

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

                buildConstOrErrorExpression(
                    sourceElement,
                    kind,
                    number,
                    ConeSimpleDiagnostic("Incorrect integer literal: $text", diagnostic)
                )
            }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    buildConstOrErrorExpression(
                        sourceElement,
                        ConstantValueKind.Float,
                        convertedText,
                        ConeSimpleDiagnostic("Incorrect float: $text", DiagnosticKind.FloatLiteralOutOfRange)
                    )
                } else {
                    buildConstOrErrorExpression(
                        sourceElement,
                        ConstantValueKind.Double,
                        convertedText as? Double,
                        ConeSimpleDiagnostic("Incorrect double: $text", DiagnosticKind.FloatLiteralOutOfRange)
                    )
                }
            CHARACTER_CONSTANT -> {
                val characterWithDiagnostic = text.parseCharacter()
                buildConstOrErrorExpression(
                    sourceElement,
                    ConstantValueKind.Char,
                    characterWithDiagnostic.value,
                    ConeSimpleDiagnostic(
                        "Incorrect character: $text",
                        characterWithDiagnostic.getDiagnostic() ?: DiagnosticKind.IllegalConstExpression
                    )
                )
            }
            BOOLEAN_CONSTANT ->
                buildLiteralExpression(
                    sourceElement,
                    ConstantValueKind.Boolean,
                    convertedText as Boolean,
                    setType = false
                )
            NULL ->
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

    protected fun ExceptionAttachmentBuilder.withSourceElementEntry(name: String, element: T?) {
        when (element) {
            is PsiElement -> withPsiEntry(name, element)
            else -> withEntry(name, element) { it.asText }
        }
    }


    fun convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary(
        source: T,
        receiver: FirExpression,
        operationToken: IElementType,
    ): FirExpression? {
        if (receiver !is FirLiteralExpression) return null
        if (receiver.kind != ConstantValueKind.IntegerLiteral) return null
        if (operationToken != PLUS && operationToken != MINUS) return null

        val value = receiver.value as Long
        val convertedValue = when (operationToken) {
            MINUS -> -value
            PLUS -> value
            else -> error("Should not be here")
        }

        return buildLiteralExpression(
            source.toFirSourceElement(),
            ConstantValueKind.IntegerLiteral,
            convertedValue,
            setType = false
        )
    }

    fun Array<out T?>.toInterpolatingCall(
        base: T,
        getElementType: (T) -> IElementType = { it.elementType },
        convertTemplateEntry: T?.(String) -> Collection<FirExpression>,
        prefix: () -> String,
    ): FirExpression {
        return buildStringConcatenationCall {
            val sb = StringBuilder()
            var hasExpressions = false
            argumentList = buildArgumentList {
                L@ for (entry in this@toInterpolatingCall) {
                    if (entry == null) continue
                    when (getElementType(entry)) {
                        INTERPOLATION_PREFIX, OPEN_QUOTE, CLOSING_QUOTE -> continue@L
                        LITERAL_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.asText)
                            arguments += buildLiteralExpression(
                                entry.toFirSourceElement(), ConstantValueKind.String, entry.asText, setType = false
                            )
                        }
                        ESCAPE_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.unescapedValue)
                            val characterWithDiagnostic = escapedStringToCharacter(entry.asText)
                            arguments += buildConstOrErrorExpression(
                                entry.toFirSourceElement(),
                                ConstantValueKind.String,
                                characterWithDiagnostic.value?.toString(),
                                ConeSimpleDiagnostic(
                                    "Incorrect character: ${entry.asText}",
                                    characterWithDiagnostic.getDiagnostic() ?: DiagnosticKind.IllegalConstExpression
                                )
                            )
                        }
                        SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                            hasExpressions = true
                            val expressions = entry.convertTemplateEntry("Incorrect template argument")
                            if (expressions.isNotEmpty()) {
                                arguments += expressions
                            } else {
                                arguments += buildErrorExpression {
                                    source = entry.toFirSourceElement()
                                    diagnostic = ConeSyntaxDiagnostic("Incorrect template argument")
                                }
                            }
                        }
                        else -> {
                            hasExpressions = true
                            arguments += buildErrorExpression {
                                source = entry.toFirSourceElement()
                                diagnostic = ConeSyntaxDiagnostic("Incorrect template entry: ${entry.asText}")
                            }
                        }
                    }
                }
            }
            source = base?.toFirSourceElement()
            interpolationPrefix = prefix()
            // Fast-pass if there is no errors and non-const string expressions
            if (!hasExpressions && !argumentList.arguments.any { it is FirErrorExpression })
                return buildLiteralExpression(
                    source,
                    ConstantValueKind.String,
                    sb.toString(),
                    setType = false,
                    prefix = interpolationPrefix.takeIf { it.isNotEmpty() }
                )
        }
    }

    fun generateIncrementOrDecrementBlock(
        // Used to obtain source-element or text
        wholeExpression: T,
        operationReference: T?,
        receiver: T?,
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression,
    ): FirExpression {
        val unwrappedReceiver = receiver.unwrap() ?: return buildErrorExpression {
            diagnostic = ConeSyntaxDiagnostic("Inc/dec without operand")
        }

        if (unwrappedReceiver.elementType == ARRAY_ACCESS_EXPRESSION) {
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
            val baseSource = wholeExpression?.toFirSourceElement()
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


    private fun T?.unwrap(): T? {
        // NOTE: By removing surrounding parentheses and labels, FirLabels will NOT be created for those labels.
        // This should be fine since the label is meaningless and unusable for a ++/-- argument or assignment LHS.
        var unwrapped = this
        while (true) {
            unwrapped = when (unwrapped?.elementType) {
                PARENTHESIZED -> unwrapped?.getExpressionInParentheses()
                LABELED_EXPRESSION -> unwrapped?.getLabeledExpression()
                ANNOTATED_EXPRESSION -> unwrapped?.getAnnotatedExpression()
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
        wholeExpression: T,
        operationReference: T?,
        receiver: T,
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression,
    ): FirExpression {
        val array = receiver.arrayExpression
        val isInc = when (callName) {
            OperatorNameConventions.INC -> true
            OperatorNameConventions.DEC -> false
            else -> error("Unexpected operator: $callName")
        }
        val sourceKind = sourceKindForIncOrDec(callName, prefix)
        val receiverSourceElement = receiver.toFirSourceElement()
        return buildBlockPossiblyUnderSafeCall(
            array, convert,
            // For (a?.b[3])++ and (a?.b)[3]++ we should not pull `++` inside safe call
            isChildInParentheses = receiverSourceElement.isChildInParentheses() || array?.toFirSourceElement()?.isChildInParentheses() == true,
            sourceElementForError = receiverSourceElement,
        ) { arrayReceiver ->
            val baseSource = wholeExpression?.toFirSourceElement()
            val desugaredSource = baseSource?.fakeElement(sourceKind)
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
                    val fakeSource = receiver?.toFirSourceElement(sourceKind)
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
                explicitReceiver = generateResolvedAccessExpression(arrayVariable.source, arrayVariable)
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
                statements += buildGetCall(
                    if (isInc) {
                        KtFakeSourceElementKind.DesugaredPrefixIncSecondGetReference
                    } else {
                        KtFakeSourceElementKind.DesugaredPrefixDecSecondGetReference
                    }
                )
            } else {
                val initialValueVar = generateTemporaryVariable(
                    baseModuleData,
                    desugaredSource,
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
        receiver: T?,
        convert: T.() -> FirExpression,
        isChildInParentheses: Boolean,
        sourceElementForError: KtSourceElement?,
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

    // T is a PSI or a light-tree node
    @OptIn(FirContractViolation::class)
    fun T?.generateAssignment(
        baseSource: KtSourceElement?,
        arrayAccessSource: KtSourceElement?,
        rhsExpression: FirExpression,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        // Effectively `value = rhs?.convert()`, but at generateIndexedAccessAugmentedAssignment we need to recreate FIR for rhs
        // since there should be different nodes for desugaring as `.set(.., get().plus($rhs1))` and `.get(...).plusAssign($rhs2)`
        // Once KT-50861 is fixed, those two parameters shall be eliminated
        rhsAST: T?,
        convert: T.() -> FirExpression,
    ): FirStatement {
        val unwrappedLhs = this.unwrap() ?: return buildErrorExpression {
            diagnostic = ConeSyntaxDiagnostic("Inc/dec without operand")
        }

        val tokenType = unwrappedLhs.elementType
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
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
                            actualReceiver, baseSource, arrayAccessSource, operation, annotations, rhsAST, convert
                        )
                    }
                    source = result.source?.fakeElement(KtFakeSourceElementKind.IndexedAssignmentCoercionBlock)
                    statements += result
                }
                statements += buildUnitExpression {
                    source = this@buildBlock.source?.fakeElement(KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion)
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
                    source = null
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.ExpressionExpected
                    )
                }

            return buildPossiblyUnderSafeCall(
                receiverToUse,
                // For (a?.b) += 1 we don't want to pull `+=` under a safe call
                isReceiverIsWrappedWithParentheses = lhsReceiver?.isChildInParentheses() == true,
                sourceElementForErrorIfSafeCallSelectorIsNotExpression = null
            ) { actualReceiver ->
                buildAugmentedAssignment {
                    source = baseSource
                    this.operation = operation
                    leftArgument = actualReceiver
                    rightArgument = rhsExpression
                    this.annotations += annotations
                }
            }
        }
        require(operation == FirOperation.ASSIGN)

        @Suppress("SENSELESS_COMPARISON") // K2 warning suppression, TODO: KT-62472
        if (this?.elementType == SAFE_ACCESS_EXPRESSION && this != null) {
            val safeCallNonAssignment = convert() as? FirSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, rhsExpression, annotations)
            }
        }

        val assignmentLValue = unwrappedLhs.convert()
        return buildVariableAssignment {
            source = baseSource
            lValue = if (baseSource?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
                buildDesugaredAssignmentValueReferenceExpression {
                    expressionRef = FirExpressionRef<FirExpression>().apply { bind(assignmentLValue) }
                    source = assignmentLValue.source?.fakeElement(baseSource.kind as KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
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
        baseSource: KtSourceElement?,
        arrayAccessSource: KtSourceElement?,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        rhs: T?,
        convert: T.() -> FirExpression,
    ): FirStatement {
        // For case of LHS is a parenthesized safe call, like (a?.b[3]) += 1
        // Here, we explicitly declare that it can't be desugared as `a?.{ b[3] = b[3] + 1 }` or
        // as some other sort of `plus` + set, thus we leave only `plusAssign` form.
        if (receiver is FirSafeCallExpression) {
            return buildFunctionCall {
                this.source = source
                explicitReceiver = receiver
                argumentList = buildUnaryArgumentList(
                    rhs?.convert() ?: buildErrorExpression(
                        null,
                        ConeSyntaxDiagnostic("No value for array set")
                    )
                )

                calleeReference = buildSimpleNamedReference {
                    this.source = baseSource
                    this.name = FirOperationNameConventions.ASSIGNMENTS.getValue(operation)
                }
                origin = FirFunctionCallOrigin.Operator
                this.annotations.addAll(annotations)
            }
        }

        require(receiver is FirFunctionCall) {
            "Array access should be desugared to a function call, but $receiver is found"
        }

        return buildIndexedAccessAugmentedAssignment {
            source = baseSource
            this.operation = operation
            this.lhsGetCall = receiver
            this.rhs = rhs?.convert() ?: buildErrorExpression(
                null,
                ConeSyntaxDiagnostic("No value for array set")
            )
            this.arrayAccessSource = arrayAccessSource
            this.annotations += annotations
        }
    }

    inner class DataClassMembersGenerator(
        private val source: T,
        private val classBuilder: FirRegularClassBuilder,
        private val zippedParameters: List<Pair<T, FirProperty>>,
        private val packageFqName: FqName,
        private val classFqName: FqName,
        private val createClassTypeRefWithSourceKind: (KtFakeSourceElementKind) -> FirTypeRef,
        private val createParameterTypeRefWithSourceKind: (FirProperty, KtFakeSourceElementKind) -> FirTypeRef,
        private val addValueParameterAnnotations: FirValueParameterBuilder.(T) -> Unit,
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
            for ((sourceNode, firProperty) in zippedParameters) {
                if (!firProperty.isVal && !firProperty.isVar) continue
                val name = Name.identifier("component$componentIndex")
                componentIndex++
                val componentFunction = buildSimpleFunction {
                    source = sourceNode?.toFirSourceElement(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Synthetic.DataClassMember
                    returnTypeRef = firProperty.returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    this.name = name
                    status = FirDeclarationStatusImpl(firProperty.visibility, Modality.FINAL).apply {
                        isOperator = true
                    }
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
                    createClassTypeRefWithSourceKind,
                    createParameterTypeRefWithSourceKind,
                    { src, kind -> src?.toFirSourceElement(kind) },
                    addValueParameterAnnotations,
                    { it.isVararg },
                )
            )
        }
    }

    protected fun FirRegularClass.initContainingClassForLocalAttr() {
        if (isLocal) {
            val currentDispatchReceiverType = currentDispatchReceiverType()
            if (currentDispatchReceiverType != null) {
                containingClassForLocalAttr = currentDispatchReceiverType.lookupTag
            }
        }
    }

    protected fun FirRegularClassBuilder.initCompanionObjectSymbolAttr() {
        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol
    }

    protected fun FirCallableDeclaration.initContainingClassAttr() {
        initContainingClassAttr(context)
    }

    protected fun buildLabel(rawName: String, source: KtSourceElement): FirLabel {
        val firLabel = buildLabel {
            name = KtPsiUtil.unquoteIdentifier(rawName)
            this.source = source
        }

        return firLabel
    }

    protected fun getForbiddenLabelKind(rawName: String, isMultipleLabel: Boolean): ForbiddenLabelKind? = when {
        rawName.isUnderscore -> ForbiddenLabelKind.UNDERSCORE_IS_RESERVED
        isMultipleLabel -> ForbiddenLabelKind.MULTIPLE_LABEL
        else -> null
    }

    protected enum class ForbiddenLabelKind {
        UNDERSCORE_IS_RESERVED, MULTIPLE_LABEL
    }

    protected fun buildExpressionHandlingErrors(
        element: FirElement?,
        elementSource: KtSourceElement,
        forbiddenLabelKind: ForbiddenLabelKind?,
        forbiddenLabelSource: KtSourceElement?,
    ): FirElement {
        return if (element != null) {
            if (forbiddenLabelKind != null) {
                require(forbiddenLabelSource != null)
                buildErrorExpression {
                    this.source = element.source
                    this.expression = element as? FirExpression
                    diagnostic = when (forbiddenLabelKind) {
                        ForbiddenLabelKind.UNDERSCORE_IS_RESERVED -> ConeUnderscoreIsReserved(forbiddenLabelSource)
                        ForbiddenLabelKind.MULTIPLE_LABEL -> ConeMultipleLabelsAreForbidden(forbiddenLabelSource)
                    }
                }
            } else {
                element
            }
        } else {
            buildErrorExpression(elementSource, ConeSyntaxDiagnostic("Empty label"))
        }
    }

    protected fun convertFirSelector(
        firSelector: FirQualifiedAccessExpression,
        source: KtSourceElement?,
        receiver: FirExpression,
    ): FirQualifiedAccessExpression {
        return if (firSelector is FirImplicitInvokeCall) {
            buildImplicitInvokeCall {
                this.source = source
                annotations.addAll(firSelector.annotations)
                typeArguments.addAll(firSelector.typeArguments)
                explicitReceiver = firSelector.explicitReceiver
                argumentList = buildArgumentList {
                    arguments.add(receiver)
                    arguments.addAll(firSelector.arguments)
                }
                isCallWithExplicitReceiver = true
                calleeReference = firSelector.calleeReference
            }
        } else {
            firSelector.replaceExplicitReceiver(receiver)
            @OptIn(FirImplementationDetail::class)
            firSelector.replaceSource(source)
            firSelector
        }
    }

    protected fun convertValueParameterName(
        safeName: Name,
        valueParameterDeclaration: ValueParameterDeclaration,
        rawName: () -> String?,
    ): Name {
        return if (valueParameterDeclaration == ValueParameterDeclaration.LAMBDA && rawName() == "_"
            ||
            valueParameterDeclaration == ValueParameterDeclaration.CATCH &&
            safeName.asString() == "_"
        ) {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        } else {
            safeName
        }
    }

    protected fun buildErrorTopLevelDestructuringDeclaration(source: KtSourceElement): FirErrorProperty = buildErrorProperty {
        this.source = source
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Source
        name = Name.special("<destructuring>")
        diagnostic = ConeDestructuringDeclarationsOnTopLevel
        symbol = FirErrorPropertySymbol(diagnostic)
    }

    protected fun createNoTypeForParameterTypeRef(parameterSource: KtSourceElement): FirErrorTypeRef {
        return buildErrorTypeRef {
            source = parameterSource
            diagnostic = ConeSimpleDiagnostic("No type for parameter", DiagnosticKind.ValueParameterWithNoTypeAnnotation)
        }
    }

    enum class ValueParameterDeclaration(val shouldExplicitParameterTypeBePresent: Boolean) {
        FUNCTION(shouldExplicitParameterTypeBePresent = true),
        CATCH(shouldExplicitParameterTypeBePresent = true),
        PRIMARY_CONSTRUCTOR(shouldExplicitParameterTypeBePresent = true),
        SETTER(shouldExplicitParameterTypeBePresent = false),
        LAMBDA(shouldExplicitParameterTypeBePresent = false),
        FOR_LOOP(shouldExplicitParameterTypeBePresent = false),
    }
}

fun <TBase, TSource : TBase, TParameter : TBase> FirRegularClassBuilder.createDataClassCopyFunction(
    classId: ClassId,
    sourceElement: TSource,
    dispatchReceiver: ConeClassLikeType?,
    zippedParameters: List<Pair<TParameter, FirProperty>>,
    isFromLibrary: Boolean,
    createClassTypeRefWithSourceKind: (KtFakeSourceElementKind) -> FirTypeRef,
    createParameterTypeRefWithSourceKind: (FirProperty, KtFakeSourceElementKind) -> FirTypeRef,
    toFirSource: (TBase?, KtFakeSourceElementKind) -> KtSourceElement?,
    addValueParameterAnnotations: FirValueParameterBuilder.(TParameter) -> Unit,
    isVararg: (TParameter) -> Boolean,
): FirSimpleFunction {
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

    return buildSimpleFunction {
        val classTypeRef = createClassTypeRefWithSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers)
        this.source = toFirSource(sourceElement, KtFakeSourceElementKind.DataClassGeneratedMembers)
        moduleData = this@createDataClassCopyFunction.moduleData
        origin = declarationOrigin
        returnTypeRef = classTypeRef
        name = StandardNames.DATA_CLASS_COPY
        symbol = FirNamedFunctionSymbol(CallableId(classId.packageFqName, classId.relativeClassName, StandardNames.DATA_CLASS_COPY))
        dispatchReceiverType = dispatchReceiver
        resolvePhase = this@createDataClassCopyFunction.resolvePhase
        // We need to resolve annotations on the data class. It's not possible to do it on RAW_FIR phase.
        // We will resolve the visibility later in the STATUS phase
        status = if (isFromLibrary) {
            FirResolvedDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL, EffectiveVisibility.Unknown)
        } else {
            FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
        }
        for ((ktParameter, firProperty) in zippedParameters) {
            val propertyName = firProperty.name
            val parameterSource = toFirSource(ktParameter, KtFakeSourceElementKind.DataClassGeneratedMembers)
            val propertyReturnTypeRef =
                createParameterTypeRefWithSourceKind(firProperty, KtFakeSourceElementKind.DataClassGeneratedMembers)
            valueParameters += buildValueParameter {
                resolvePhase = this@createDataClassCopyFunction.resolvePhase
                source = parameterSource
                containingFunctionSymbol = this@buildSimpleFunction.symbol
                moduleData = this@createDataClassCopyFunction.moduleData
                origin = declarationOrigin
                returnTypeRef = propertyReturnTypeRef
                name = propertyName
                symbol = FirValueParameterSymbol(propertyName)
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
fun List<FirAnnotationCall>.filterConstructorPropertyRelevantAnnotations(isVar: Boolean) = filter {
    it.useSiteTarget == null || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY
            || !isVar && (it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER)
}

fun List<FirAnnotationCall>.filterStandalonePropertyRelevantAnnotations(isVar: Boolean) = filter {
    it.useSiteTarget != AnnotationUseSiteTarget.FIELD && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_GETTER &&
            (!isVar || it.useSiteTarget != AnnotationUseSiteTarget.SETTER_PARAMETER && it.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_SETTER)
}
