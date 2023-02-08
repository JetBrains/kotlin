/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.parsing.*
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

//T can be either PsiElement, or LighterASTNode
abstract class BaseFirBuilder<T>(val baseSession: FirSession, val context: Context<T> = Context()) {
    val baseModuleData: FirModuleData = baseSession.moduleData

    abstract fun T.toFirSourceElement(kind: KtFakeSourceElementKind? = null): KtSourceElement

    protected val implicitUnitType = baseSession.builtinTypes.unitType
    protected val implicitAnyType = baseSession.builtinTypes.anyType
    protected val implicitEnumType = baseSession.builtinTypes.enumType
    protected val implicitAnnotationType = baseSession.builtinTypes.annotationType

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

    /**** Class name utils ****/
    inline fun <T> withChildClassName(
        name: Name,
        isExpect: Boolean,
        forceLocalContext: Boolean = false,
        l: () -> T
    ): T {
        context.className = context.className.child(name)
        val oldForcedLocalContext = context.forcedLocalContext
        context.forcedLocalContext = forceLocalContext || context.forcedLocalContext
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
            context.forcedLocalContext = oldForcedLocalContext
            context.containerIsExpect = previousIsExpect
        }
    }

    fun registerSelfType(selfType: FirResolvedTypeRef) {
        context.dispatchReceiverTypesStack.add(selfType.type as ConeClassLikeType)
    }

    protected inline fun <T> withCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement? = null,
        currentFirTypeParameters: List<FirTypeParameterRef>,
        block: () -> T
    ): T {
        addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        return try {
            block()
        } finally {
            context.popFirTypeParameters()
        }
    }

    protected open fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>
    ) {
        context.pushFirTypeParameters(status, currentFirTypeParameters)
    }

    fun callableIdForName(name: Name) =
        when {
            context.className.shortNameOrSpecial() == SpecialNames.ANONYMOUS -> CallableId(ANONYMOUS_CLASS_ID, name)
            context.className.isRoot && !context.inLocalContext -> CallableId(context.packageFqName, name)
            context.inLocalContext -> {
                val pathFqName =
                    context.firFunctionTargets.fold(
                        if (context.className.isRoot) context.packageFqName else context.currentClassId.asSingleFqName()
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

    fun callableIdForClassConstructor() =
        if (context.className == FqName.ROOT) CallableId(context.packageFqName, Name.special("<anonymous-init>"))
        else CallableId(context.packageFqName, context.className, context.className.shortName())


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
        fromKtReturnExpression: Boolean = false
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
            type = ConeClassLikeTypeImpl(
                symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }
    }

    fun constructorTypeParametersFromConstructedClass(ownerTypeParameters: List<FirTypeParameterRef>): List<FirTypeParameterRef> {
        return ownerTypeParameters.mapNotNull {
            val declaredTypeParameter = (it as? FirTypeParameter) ?: return@mapNotNull null
            buildConstructedClassTypeParameterRef { symbol = declaredTypeParameter.symbol }
        }
    }

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
                buildConstExpression(
                    sourceElement,
                    ConstantValueKind.Boolean,
                    convertedText as Boolean
                )
            NULL ->
                buildConstExpression(
                    sourceElement,
                    ConstantValueKind.Null,
                    null
                )
            else ->
                throw AssertionError("Unknown literal type: $type, $text")
        }
    }

    fun convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary(
        source: T,
        receiver: FirExpression,
        operationToken: IElementType
    ): FirExpression? {
        if (receiver !is FirConstExpression<*>) return null
        if (receiver.kind != ConstantValueKind.IntegerLiteral) return null
        if (operationToken != PLUS && operationToken != MINUS) return null

        val value = receiver.value as Long
        val convertedValue = when (operationToken) {
            MINUS -> -value
            PLUS -> value
            else -> error("Should not be here")
        }

        return buildConstExpression(
            source.toFirSourceElement(),
            ConstantValueKind.IntegerLiteral,
            convertedValue
        )
    }

    fun Array<out T?>.toInterpolatingCall(
        base: T,
        getElementType: (T) -> IElementType = { it.elementType },
        convertTemplateEntry: T?.(String) -> FirExpression
    ): FirExpression {
        return buildStringConcatenationCall {
            val sb = StringBuilder()
            var hasExpressions = false
            argumentList = buildArgumentList {
                L@ for (entry in this@toInterpolatingCall) {
                    if (entry == null) continue
                    arguments += when (getElementType(entry)) {
                        OPEN_QUOTE, CLOSING_QUOTE -> continue@L
                        LITERAL_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.asText)
                            buildConstExpression(entry.toFirSourceElement(), ConstantValueKind.String, entry.asText)
                        }
                        ESCAPE_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.unescapedValue)
                            buildConstExpression(entry.toFirSourceElement(), ConstantValueKind.String, entry.unescapedValue)
                        }
                        SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                            hasExpressions = true
                            entry.convertTemplateEntry("Incorrect template argument")
                        }
                        else -> {
                            hasExpressions = true
                            buildErrorExpression {
                                source = entry.toFirSourceElement()
                                diagnostic = ConeSimpleDiagnostic("Incorrect template entry: ${entry.asText}", DiagnosticKind.Syntax)
                            }
                        }
                    }
                }
            }
            source = base?.toFirSourceElement()
            // Fast-pass if there is no non-const string expressions
            if (!hasExpressions) return buildConstExpression(source, ConstantValueKind.String, sb.toString())
        }
    }

    /**
     * given:
     * receiver++
     *
     * result:
     * {
     *     val <unary> = receiver
     *     receiver = <unary>.inc()
     *     ^<unary>
     * }
     *
     * given:
     * ++receiver
     *
     * result:
     * {
     *     val <unary-result> = receiver.inc()
     *     receiver = <unary-result>
     *     ^<unary-result>
     * }
     *
     */

    // TODO:
    // 1. Support receiver capturing for `a?.b++` (elementType == SAFE_ACCESS_EXPRESSION).
    // 2. Add box test cases for #1 where receiver expression has side effects.
    fun generateIncrementOrDecrementBlock(
        // Used to obtain source-element or text
        wholeExpression: T,
        operationReference: T?,
        receiver: T?,
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression
    ): FirExpression {
        val unwrappedReceiver = receiver.unwrap() ?: return buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("Inc/dec without operand", DiagnosticKind.Syntax)
        }

        if (unwrappedReceiver.elementType == DOT_QUALIFIED_EXPRESSION || unwrappedReceiver.elementType == SAFE_ACCESS_EXPRESSION) {
            return generateIncrementOrDecrementBlockForQualifiedAccess(
                wholeExpression,
                operationReference,
                unwrappedReceiver,
                callName,
                prefix,
                convert
            )
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

        return buildBlock {
            val baseSource = wholeExpression?.toFirSourceElement()
            val desugaredSource = baseSource?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
            source = desugaredSource

            val convertedReceiver = unwrappedReceiver.convert()

            putIncrementOrDecrementStatements(
                convertedReceiver,
                operationReference,
                callName,
                prefix,
                unwrappedReceiver.takeIf { it.elementType == REFERENCE_EXPRESSION }?.getReferencedNameAsName(),
                desugaredSource,
            ) { resultInitializer: FirExpression, resultVar: FirVariable ->
                val assignment = unwrappedReceiver.generateAssignment(
                    desugaredSource,
                    null,
                    if (prefix && unwrappedReceiver.elementType != REFERENCE_EXPRESSION)
                        generateResolvedAccessExpression(source, resultVar)
                    else
                        resultInitializer,
                    FirOperation.ASSIGN,
                    resultInitializer.annotations,
                    null
                ) {
                    // We want the DesugaredAssignmentValueReferenceExpression on the LHS to point to the same receiver instance
                    // as in the initializer, therefore don't create a new instance here if the argument is the receiver.
                    if (this == unwrappedReceiver) convertedReceiver else convert()
                }

                if (assignment is FirBlock) {
                    statements += assignment.statements
                } else {
                    statements += assignment
                }
            }
        }
    }


    /**
     * given:
     * receiver++
     *
     * result:
     * {
     *     val <unary> = receiver
     *     val resultVar = <unary>.inc()
     *     appendAssignment(resultVar)
     *     ^<unary>
     * }
     *
     * given:
     * ++receiver
     *
     * result:
     * {
     *     val <unary-result> = receiver.inc()
     *     val resultVar = <unary-result>
     *     appendAssignment(resultVar)
     *     ^<unary-result>
     * }
     *
     */
    private fun FirBlockBuilder.putIncrementOrDecrementStatements(
        receiver: FirExpression,
        operationReference: T?,
        callName: Name, // 'inc' or 'dec'
        prefix: Boolean,
        nameIfSimpleReference: Name?, // 'b' if whole expression is simple `b++` or `a.b++`, but not `a[1]++`
        desugaredSource: KtSourceElement?,
        appendAssignment: FirBlockBuilder.(resultInitializer: FirExpression, resultVar: FirVariable) -> Unit
    ) {
        // initialValueVar is only used for postfix increment/decrement (stores the argument value before increment/decrement).
        val initialValueVar = generateTemporaryVariable(
            baseModuleData,
            desugaredSource,
            SpecialNames.UNARY,
            receiver
        )

        // resultInitializer is the expression for `argument.inc()`
        val resultInitializer = buildFunctionCall {
            source = desugaredSource
            calleeReference = buildSimpleNamedReference {
                val kind = if (prefix) {
                    KtFakeSourceElementKind.DesugaredPrefixNameReference
                } else {
                    KtFakeSourceElementKind.DesugaredPostfixNameReference
                }
                source = operationReference?.toFirSourceElement(kind)
                name = callName
            }
            explicitReceiver = if (prefix) {
                receiver
            } else {
                generateResolvedAccessExpression(desugaredSource, initialValueVar)
            }
            origin = FirFunctionCallOrigin.Operator
        }

        // resultVar is only used for prefix increment/decrement.
        val resultVar = generateTemporaryVariable(
            baseModuleData,
            desugaredSource,
            SpecialNames.UNARY_RESULT,
            resultInitializer
        )

        if (prefix) {
            if (nameIfSimpleReference != null) {
                appendAssignment(resultInitializer, resultVar)
                statements += generateAccessExpression(desugaredSource, desugaredSource, nameIfSimpleReference)
            } else {
                statements += resultVar
                appendAssignment(resultInitializer, resultVar)
                statements += generateResolvedAccessExpression(desugaredSource, resultVar)
            }
        } else {
            statements += initialValueVar
            appendAssignment(resultInitializer, resultVar)
            statements += generateResolvedAccessExpression(desugaredSource, initialValueVar)
        }
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
     * a.b++
     *
     * result:
     * {
     *     val <receiver> = a
     *     val <unary> = <receiver>.b
     *     <receiver>.b = <unary>.inc()
     *     ^<unary>
     * }
     *
     * given:
     * ++a.b
     *
     * result:
     * {
     *     val <receiver> = a
     *     val <unary-result> = <receiver>.b.inc()
     *     <receiver>.b = <unary-result>
     *     ^<unary-result>
     * }
     *
     */
    @OptIn(FirContractViolation::class)
    private fun generateIncrementOrDecrementBlockForQualifiedAccess(
        wholeExpression: T,
        operationReference: T?,
        receiverForOperation: T, // a.b
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression
    ): FirExpression {
        return buildBlockProbablyUnderSafeCall(
            receiverForOperation,
            convert,
            receiverForOperation.toFirSourceElement(),
        ) { qualifiedFir ->
            val receiverFir = (qualifiedFir as? FirQualifiedAccessExpression)?.explicitReceiver ?: buildErrorExpression {
                source = receiverForOperation.toFirSourceElement()
                diagnostic = ConeSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax)
            }

            val baseSource = wholeExpression?.toFirSourceElement()
            val desugaredSource = baseSource?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
            source = desugaredSource

            val argumentReceiverVariable = generateTemporaryVariable(
                baseModuleData,
                desugaredSource,
                SpecialNames.RECEIVER,
                initializer = receiverFir,
            ).also { statements += it }

            val receiverSource = receiverFir.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
            val firArgument = generateResolvedAccessExpression(receiverSource, argumentReceiverVariable).let { receiver ->
                qualifiedFir.also { if (it is FirQualifiedAccessExpression) it.replaceExplicitReceiver(receiver) }
            }

            putIncrementOrDecrementStatements(
                firArgument, operationReference, callName, prefix,
                nameIfSimpleReference = null, desugaredSource
            ) { resultInitializer: FirExpression, resultVar: FirVariable ->
                if (firArgument !is FirQualifiedAccessExpression) return@putIncrementOrDecrementStatements
                statements += buildVariableAssignment {
                    source = desugaredSource
                    lValue = buildDesugaredAssignmentValueReferenceExpression {
                        expressionRef = FirExpressionRef<FirExpression>().apply { bind(firArgument) }
                        source = firArgument.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
                    }
                    rValue = if (prefix) {
                        generateResolvedAccessExpression(source, resultVar)
                    } else {
                        resultInitializer
                    }
                }
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
     *     val <unary> = <array>.get(b, c)
     *     <array>.set(b, c, <unary>.inc())
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
     *     val <unary-result> = <array>.get(b, c).inc()
     *     <array>.set(b, c, <unary-result>)
     *     ^<unary-result>
     * }
     *
     */
    private fun generateIncrementOrDecrementBlockForArrayAccess(
        wholeExpression: T,
        operationReference: T?,
        receiver: T,
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression
    ): FirExpression {
        val array = receiver.arrayExpression
        return buildBlockProbablyUnderSafeCall(
            array, convert, receiver.toFirSourceElement(),
        ) { arrayReceiver ->
            val baseSource = wholeExpression?.toFirSourceElement()
            val desugaredSource = baseSource?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
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

            val firArgument = buildFunctionCall {
                source = desugaredSource
                calleeReference = buildSimpleNamedReference {
                    source = receiver?.toFirSourceElement(KtFakeSourceElementKind.ArrayAccessNameReference)
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

            putIncrementOrDecrementStatements(
                firArgument, operationReference, callName, prefix,
                nameIfSimpleReference = null, desugaredSource
            ) { resultInitializer: FirExpression, resultVar: FirVariable ->
                statements += buildFunctionCall {
                    source = desugaredSource
                    calleeReference = buildSimpleNamedReference {
                        source = receiver.toFirSourceElement()
                        name = OperatorNameConventions.SET
                    }
                    explicitReceiver = generateResolvedAccessExpression(arrayVariable.source, arrayVariable)
                    argumentList = buildArgumentList {
                        for (indexVar in indexVariables) {
                            arguments += generateResolvedAccessExpression(indexVar.source, indexVar)
                        }
                        arguments += if (prefix) {
                            generateResolvedAccessExpression(source, resultVar)
                        } else {
                            resultInitializer
                        }
                    }
                    origin = FirFunctionCallOrigin.Operator
                }
            }
        }
    }


    // if `receiver` is a safe call a?.f(...), insert a block under safe call
    // a?.{ val receiver = $subj$.f() ... } where `...` is generated by `init(FIR<$subj$.f()>)`
    //
    // Otherwise just returns buildBlock { init(FIR<receiver>)) }
    private fun buildBlockProbablyUnderSafeCall(
        receiver: T?,
        convert: T.() -> FirExpression,
        sourceElementForError: KtSourceElement?,
        init: FirBlockBuilder.(receiver: FirExpression) -> Unit = {}
    ): FirExpression {
        val receiverFir = receiver?.convert() ?: buildErrorExpression {
            source = sourceElementForError
            diagnostic = ConeSimpleDiagnostic("No receiver expression", DiagnosticKind.Syntax)
        }

        if (receiverFir is FirSafeCallExpression) {
            receiverFir.replaceSelector(
                buildBlock {
                    init(
                        receiverFir.selector as? FirExpression ?: buildErrorExpression {
                            source = sourceElementForError
                            diagnostic = ConeSimpleDiagnostic("Safe call selector expected to be an expression here", DiagnosticKind.Syntax)
                        }
                    )
                }
            )

            return receiverFir
        }

        return buildBlock {
            init(receiverFir)
        }
    }

    // T is a PSI or a light-tree node
    @OptIn(FirContractViolation::class)
    fun T?.generateAssignment(
        baseSource: KtSourceElement?,
        arrayAccessSource: KtSourceElement?,
        rhsExpression: FirExpression,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        // Effectively `value = rhs?.convert()`, but at generateAugmentedArraySetCall we need to recreate FIR for rhs
        // since there should be different nodes for desugaring as `.set(.., get().plus($rhs1))` and `.get(...).plusAssign($rhs2)`
        // Once KT-50861 is fixed, those two parameters shall be eliminated
        rhsAST: T?,
        convert: T.() -> FirExpression
    ): FirStatement {
        val unwrappedLhs = this.unwrap() ?: return buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("Inc/dec without operand", DiagnosticKind.Syntax)
        }

        val tokenType = unwrappedLhs.elementType
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
            if (operation == FirOperation.ASSIGN) {
                context.arraySetArgument[unwrappedLhs] = rhsExpression
            }
            return if (operation == FirOperation.ASSIGN) {
                val result = unwrappedLhs.convert()
                result.replaceAnnotations(result.annotations.smartPlus(annotations))
                result.pullUpSafeCallIfNecessary()
            } else {
                val receiver = unwrappedLhs.convert()

                if (receiver is FirSafeCallExpression) {
                    receiver.replaceSelector(
                        generateAugmentedArraySetCall(
                            receiver.selector as FirExpression, baseSource, arrayAccessSource, operation, annotations, rhsAST, convert
                        )
                    )
                    receiver
                } else {
                    generateAugmentedArraySetCall(receiver, baseSource, arrayAccessSource, operation, annotations, rhsAST, convert)
                }
            }
        }

        if (operation in FirOperation.ASSIGNMENTS && operation != FirOperation.ASSIGN) {
            val lhsReceiver = this@generateAssignment?.convert()

            val receiverToUse =
                if (lhsReceiver is FirSafeCallExpression)
                    lhsReceiver.selector as? FirExpression
                else
                    lhsReceiver

            val result = buildAssignmentOperatorStatement {
                source = baseSource
                this.operation = operation
                leftArgument = receiverToUse ?: buildErrorExpression {
                    source = null
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.ExpressionExpected
                    )
                }
                rightArgument = rhsExpression
                this.annotations += annotations
            }

            return if (lhsReceiver is FirSafeCallExpression) {
                lhsReceiver.replaceSelector(result)
                lhsReceiver
            } else {
                result
            }
        }
        require(operation == FirOperation.ASSIGN)

        if (this?.elementType == SAFE_ACCESS_EXPRESSION && this != null) {
            val safeCallNonAssignment = convert() as? FirSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, rhsExpression, annotations)
            }
        }

        val assignmentLValue = unwrappedLhs.convert()
        return buildVariableAssignment {
            source = baseSource
            lValue = if (baseSource?.kind == KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
                buildDesugaredAssignmentValueReferenceExpression {
                    expressionRef = FirExpressionRef<FirExpression>().apply { bind(assignmentLValue) }
                    source = assignmentLValue.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
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
        annotations: List<FirAnnotation>
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

    private fun generateAugmentedArraySetCall(
        receiver: FirExpression, // a.get(x,y)
        baseSource: KtSourceElement?,
        arrayAccessSource: KtSourceElement?,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        rhs: T?,
        convert: T.() -> FirExpression
    ): FirStatement {
        require(receiver is FirFunctionCall) {
            "Array access should be desugared to a function call, but $receiver is found"
        }

        return buildAugmentedArraySetCall {
            source = baseSource
            this.operation = operation
            this.lhsGetCall = receiver
            this.rhs = rhs?.convert() ?: buildErrorExpression(
                null,
                ConeSimpleDiagnostic("No value for array set", DiagnosticKind.Syntax)
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
    ) {
        fun generate() {
            if (classBuilder.classKind != ClassKind.OBJECT) {
                generateComponentFunctions()
                generateCopyFunction()
            }
            // Refer to (IR utils or FIR backend) DataClassMembersGenerator for generating equals, hashCode, and toString
        }

        private fun generateComponentAccess(
            parameterSource: KtSourceElement?,
            firProperty: FirProperty,
            classTypeRefWithCorrectSourceKind: FirTypeRef,
            firPropertyReturnTypeRefWithCorrectSourceKind: FirTypeRef
        ) =
            buildPropertyAccessExpression {
                source = parameterSource
                typeRef = firPropertyReturnTypeRefWithCorrectSourceKind
                dispatchReceiver = buildThisReceiverExpression {
                    source = parameterSource
                    calleeReference = buildImplicitThisReference {
                        boundSymbol = classBuilder.symbol
                    }
                    typeRef = classTypeRefWithCorrectSourceKind
                }
                calleeReference = buildResolvedNamedReference {
                    source = parameterSource
                    this.name = firProperty.name
                    resolvedSymbol = firProperty.symbol
                }
            }

        private fun generateComponentFunctions() {
            var componentIndex = 1
            for ((sourceNode, firProperty) in zippedParameters) {
                if (!firProperty.isVal && !firProperty.isVar) continue
                val name = Name.identifier("component$componentIndex")
                componentIndex++
                val parameterSource = sourceNode?.toFirSourceElement()
                val componentFunction = buildSimpleFunction {
                    source = parameterSource?.fakeElement(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = firProperty.returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    this.name = name
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                        isOperator = true
                    }
                    symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))
                    dispatchReceiverType = currentDispatchReceiverType()
                    // Refer to FIR backend ClassMemberGenerator for body generation.
                }
                classBuilder.addDeclaration(componentFunction)
            }
        }

        private val copyName = Name.identifier("copy")

        private fun generateCopyFunction() {
            classBuilder.addDeclaration(
                buildSimpleFunction {
                    val classTypeRef = createClassTypeRefWithSourceKind(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    source = this@DataClassMembersGenerator.source.toFirSourceElement(KtFakeSourceElementKind.DataClassGeneratedMembers)
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = classTypeRef
                    name = copyName
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                    symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, copyName))
                    dispatchReceiverType = currentDispatchReceiverType()
                    for ((ktParameter, firProperty) in zippedParameters) {
                        val propertyName = firProperty.name
                        val parameterSource = ktParameter?.toFirSourceElement(KtFakeSourceElementKind.DataClassGeneratedMembers)
                        val propertyReturnTypeRef =
                            createParameterTypeRefWithSourceKind(firProperty, KtFakeSourceElementKind.DataClassGeneratedMembers)
                        valueParameters += buildValueParameter {
                            source = parameterSource
                            containingFunctionSymbol = this@buildSimpleFunction.symbol
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = propertyReturnTypeRef
                            name = propertyName
                            symbol = FirValueParameterSymbol(propertyName)
                            defaultValue = generateComponentAccess(parameterSource, firProperty, classTypeRef, propertyReturnTypeRef)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                    }
                    // Refer to FIR backend ClassMemberGenerator for body generation.
                }
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

    protected inline fun <R> withDefaultSourceElementKind(newDefault: KtSourceElementKind, action: () -> R): R {
        val currentForced = context.forcedElementSourceKind
        context.forcedElementSourceKind = newDefault
        try {
            return action()
        } finally {
            context.forcedElementSourceKind = currentForced
        }
    }

    protected fun buildLabelAndErrorSource(rawName: String, source: KtSourceElement): Pair<FirLabel, KtSourceElement?> {
        val firLabel = buildLabel {
            name = KtPsiUtil.unquoteIdentifier(rawName)
            this.source = source
        }

        return Pair(firLabel, if (rawName.isUnderscore) firLabel.source else null)
    }

    protected fun buildExpressionWithErrorLabel(
        element: FirElement?,
        errorLabelSource: KtSourceElement?,
        elementSource: KtSourceElement
    ): FirElement {
        return if (element != null) {
            if (errorLabelSource != null) {
                buildErrorExpression {
                    this.source = element.source
                    this.expression = element as? FirExpression
                    diagnostic = ConeUnderscoreIsReserved(errorLabelSource)
                }
            } else {
                element
            }
        } else {
            buildErrorExpression(elementSource, ConeSimpleDiagnostic("Empty label", DiagnosticKind.Syntax))
        }
    }

    protected fun convertFirSelector(
        firSelector: FirQualifiedAccessExpression,
        source: KtSourceElement?,
        receiver: FirExpression
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

    protected fun buildErrorTopLevelDestructuringDeclaration(source: KtSourceElement) = buildErrorProperty {
        this.source = source
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Source
        name = Name.special("<destructuring>")
        diagnostic = ConeDestructuringDeclarationsOnTopLevel
        symbol = FirErrorPropertySymbol(diagnostic)
    }

    protected fun createNoTypeForParameterTypeRef(): FirErrorTypeRef {
        return buildErrorTypeRef {
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
