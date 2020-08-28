/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens.CLOSING_QUOTE
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_QUOTE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.*
import org.jetbrains.kotlin.util.OperatorNameConventions

//T can be either PsiElement, or LighterASTNode
abstract class BaseFirBuilder<T>(val baseSession: FirSession, val context: Context<T> = Context()) {

    abstract fun T.toFirSourceElement(kind: FirFakeSourceElementKind? = null): FirSourceElement

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
    abstract fun T.getChildNodeByType(type: IElementType): T?
    abstract val T?.selectorExpression: T?

    /**** Class name utils ****/
    inline fun <T> withChildClassName(
        name: Name,
        isLocal: Boolean = context.firFunctionTargets.isNotEmpty(),
        l: () -> T
    ): T {
        context.className = context.className.child(name)
        context.localBits.add(isLocal)
        return try {
            l()
        } finally {
            context.className = context.className.parent()
            context.localBits.removeLast()
        }
    }

    inline fun <T> withCapturedTypeParameters(block: () -> T): T {
        val previous = context.capturedTypeParameters
        val result = block()
        context.capturedTypeParameters = previous
        return result
    }

    fun addCapturedTypeParameters(typeParameters: List<FirTypeParameterRef>) {
        context.capturedTypeParameters =
            context.capturedTypeParameters.addAll(0, typeParameters.map { typeParameter -> typeParameter.symbol })
    }

    fun clearCapturedTypeParameters() {
        context.capturedTypeParameters = context.capturedTypeParameters.clear()
    }

    fun callableIdForName(name: Name, local: Boolean = false) =
        when {
            local -> {
                val pathFqName =
                    context.firFunctionTargets.fold(
                        if (context.className == FqName.ROOT) context.packageFqName else context.currentClassId.asSingleFqName()
                    ) { result, firFunctionTarget ->
                        if (firFunctionTarget.isLambda || firFunctionTarget.labelName == null)
                            result
                        else
                            result.child(Name.identifier(firFunctionTarget.labelName!!))
                    }
                CallableId(name, pathFqName)
            }
            context.className == FqName.ROOT -> CallableId(context.packageFqName, name)
            context.className.shortName() == ANONYMOUS_OBJECT_NAME -> CallableId(ANONYMOUS_CLASS_ID, name)
            else -> CallableId(context.packageFqName, context.className, name)
        }

    fun callableIdForClassConstructor() =
        if (context.className == FqName.ROOT) CallableId(context.packageFqName, Name.special("<anonymous-init>"))
        else CallableId(context.packageFqName, context.className, context.className.shortName())


    /**** Function utils ****/
    fun <T> MutableList<T>.removeLast() {
        removeAt(size - 1)
    }

    fun <T> MutableList<T>.pop(): T? {
        val result = lastOrNull()
        if (result != null) {
            removeAt(size - 1)
        }
        return result
    }

    /**** Common utils ****/
    companion object {
        val ANONYMOUS_OBJECT_NAME = Name.special("<anonymous>")
    }

    fun FirExpression.toReturn(baseSource: FirSourceElement? = source, labelName: String? = null): FirReturnExpression {
        return buildReturnExpression {
            fun FirFunctionTarget.bindToErrorFunction(message: String, kind: DiagnosticKind) {
                bind(
                    buildErrorFunction {
                        source = baseSource
                        session = this@BaseFirBuilder.baseSession
                        origin = FirDeclarationOrigin.Source
                        diagnostic = ConeSimpleDiagnostic(message, kind)
                        symbol = FirErrorFunctionSymbol()
                    }
                )
            }

            source = baseSource?.fakeElement(FirFakeSourceElementKind.ImplicitReturn)
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
                    bindToErrorFunction("Cannot bind label $labelName to a function", DiagnosticKind.UnresolvedLabel)
                }
            }
        }
    }

    fun T?.toDelegatedSelfType(firClass: FirRegularClassBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firClass, firClass.symbol)

    fun T?.toDelegatedSelfType(firObject: FirAnonymousObjectBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firObject, firObject.symbol)

    private fun T?.toDelegatedSelfType(firClass: FirClassBuilder, symbol: FirClassLikeSymbol<*>): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = this@toDelegatedSelfType?.toFirSourceElement(FirFakeSourceElementKind.ClassSelfTypeRef)
            type = ConeClassLikeTypeImpl(
                symbol.toLookupTag(),
                firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
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

    fun FirLoopBuilder.configure(generateBlock: () -> FirBlock): FirLoop {
        label = context.firLabels.pop()
        val target = FirLoopTarget(label?.name)
        context.firLoopTargets += target
        block = generateBlock()
        val loop = build()
        context.firLoopTargets.removeLast()
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
                val kind = when {
                    convertedText !is Long -> return reportIncorrectConstant(DiagnosticKind.IllegalConstExpression)

                    hasUnsignedLongSuffix(text) -> {
                        FirConstKind.UnsignedLong
                    }
                    hasLongSuffix(text) -> {
                        FirConstKind.Long
                    }
                    hasUnsignedSuffix(text) -> {
                        FirConstKind.UnsignedIntegerLiteral
                    }

                    else -> {
                        FirConstKind.IntegerLiteral
                    }
                }

                buildConstOrErrorExpression(
                    sourceElement,
                    kind,
                    convertedText,
                    ConeSimpleDiagnostic("Incorrect integer literal: $text", DiagnosticKind.IllegalConstExpression)
                )
            }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    buildConstOrErrorExpression(
                        sourceElement,
                        FirConstKind.Float,
                        convertedText,
                        ConeSimpleDiagnostic("Incorrect float: $text", DiagnosticKind.IllegalConstExpression)
                    )
                } else {
                    buildConstOrErrorExpression(
                        sourceElement,
                        FirConstKind.Double,
                        convertedText as? Double,
                        ConeSimpleDiagnostic("Incorrect double: $text", DiagnosticKind.IllegalConstExpression)
                    )
                }
            CHARACTER_CONSTANT ->
                buildConstOrErrorExpression(
                    sourceElement,
                    FirConstKind.Char,
                    text.parseCharacter(),
                    ConeSimpleDiagnostic("Incorrect character: $text", DiagnosticKind.IllegalConstExpression)
                )
            BOOLEAN_CONSTANT ->
                buildConstExpression(
                    sourceElement,
                    FirConstKind.Boolean,
                    convertedText as Boolean
                )
            NULL ->
                buildConstExpression(
                    sourceElement,
                    FirConstKind.Null,
                    null
                )
            else ->
                throw AssertionError("Unknown literal type: $type, $text")
        }
    }

    fun Array<out T?>.toInterpolatingCall(
        base: T,
        convertTemplateEntry: T?.(String) -> FirExpression
    ): FirExpression {
        return buildStringConcatenationCall {
            val sb = StringBuilder()
            var hasExpressions = false
            argumentList = buildArgumentList {
                L@ for (entry in this@toInterpolatingCall) {
                    if (entry == null) continue
                    arguments += when (entry.elementType) {
                        OPEN_QUOTE, CLOSING_QUOTE -> continue@L
                        LITERAL_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.asText)
                            buildConstExpression(entry.toFirSourceElement(), FirConstKind.String, entry.asText)
                        }
                        ESCAPE_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.unescapedValue)
                            buildConstExpression(entry.toFirSourceElement(), FirConstKind.String, entry.unescapedValue)
                        }
                        SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                            hasExpressions = true
                            val firExpression = entry.convertTemplateEntry("Incorrect template argument")
                            val source = firExpression.source?.fakeElement(FirFakeSourceElementKind.GeneratedToStringCallOnTemplateEntry)
                            buildFunctionCall {
                                this.source = source
                                explicitReceiver = firExpression
                                calleeReference = buildSimpleNamedReference {
                                    this.source = source
                                    name = Name.identifier("toString")
                                }
                            }
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
            if (!hasExpressions) return buildConstExpression(source, FirConstKind.String, sb.toString())
            argumentList.arguments.singleOrNull()?.let { return it }
        }
    }

    /**
     * given:
     * argument++
     *
     * result:
     * {
     *     val <unary> = argument
     *     argument = <unary>.inc()
     *     ^<unary>
     * }
     *
     * given:
     * ++argument
     *
     * result:
     * {
     *     val <unary> = argument
     *     argument = <unary>.inc()
     *     ^argument
     * }
     *
     */

    // TODO: Refactor, support receiver capturing in case of a.b
    fun generateIncrementOrDecrementBlock(
        baseExpression: T,
        operationReference: T?,
        argument: T?,
        callName: Name,
        prefix: Boolean,
        convert: T.() -> FirExpression
    ): FirExpression {
        if (argument == null) {
            return buildErrorExpression {
                source = argument
                diagnostic = ConeSimpleDiagnostic("Inc/dec without operand", DiagnosticKind.Syntax)
            }
        }
        return buildBlock {
            val baseSource = baseExpression?.toFirSourceElement()
            val desugaredSource = baseSource?.fakeElement(FirFakeSourceElementKind.DesugaredIncrementOrDecrement)
            source = desugaredSource
            val tempName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(
                this@BaseFirBuilder.baseSession,
                desugaredSource,
                tempName,
                argument.convert()
            )
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = buildFunctionCall {
                source = desugaredSource
                calleeReference = buildSimpleNamedReference {
                    source = operationReference?.toFirSourceElement()
                    name = callName
                }
                explicitReceiver = generateResolvedAccessExpression(desugaredSource, temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(this@BaseFirBuilder.baseSession, desugaredSource, resultName, resultInitializer)
            val assignment = argument.generateAssignment(
                desugaredSource,
                argument,
                if (prefix && argument.elementType != REFERENCE_EXPRESSION)
                    generateResolvedAccessExpression(source, resultVar)
                else
                    resultInitializer,
                FirOperation.ASSIGN, convert
            )

            fun appendAssignment() {
                if (assignment is FirBlock) {
                    statements += assignment.statements
                } else {
                    statements += assignment
                }
            }

            if (prefix) {
                if (argument.elementType != REFERENCE_EXPRESSION) {
                    statements += resultVar
                    appendAssignment()
                    statements += generateResolvedAccessExpression(desugaredSource, resultVar)
                } else {
                    appendAssignment()
                    statements += generateAccessExpression(desugaredSource, argument.getReferencedNameAsName())
                }
            } else {
                appendAssignment()
                statements += generateResolvedAccessExpression(desugaredSource, temporaryVariable)
            }
        }
    }

    private fun FirQualifiedAccessBuilder.initializeLValue(
        left: T?,
        convertQualified: T.() -> FirQualifiedAccess?
    ): FirReference {
        val tokenType = left?.elementType
        if (left != null) {
            when (tokenType) {
                REFERENCE_EXPRESSION -> {
                    return buildSimpleNamedReference {
                        source = left.toFirSourceElement()
                        name = left.getReferencedNameAsName()
                    }
                }
                THIS_EXPRESSION -> {
                    return buildExplicitThisReference {
                        source = left.toFirSourceElement()
                        labelName = left.getLabelName()
                    }
                }
                DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> {
                    val firMemberAccess = left.convertQualified()
                    return if (firMemberAccess != null) {
                        explicitReceiver = firMemberAccess.explicitReceiver
                        firMemberAccess.calleeReference
                    } else {
                        buildErrorNamedReference {
                            source = left.toFirSourceElement()
                            diagnostic = ConeSimpleDiagnostic("Unsupported qualified LValue: ${left.asText}", DiagnosticKind.Syntax)
                        }
                    }
                }
                PARENTHESIZED -> {
                    return initializeLValue(left.getExpressionInParentheses(), convertQualified)
                }
                ANNOTATED_EXPRESSION -> {
                    return initializeLValue(left.getAnnotatedExpression(), convertQualified)
                }
            }
        }
        return buildErrorNamedReference {
            source = left?.toFirSourceElement()
            diagnostic = ConeSimpleDiagnostic("Unsupported LValue: $tokenType", DiagnosticKind.VariableExpected)
        }
    }

    fun T?.generateAssignment(
        baseSource: FirSourceElement?,
        rhs: T?,
        value: FirExpression, // value is FIR for rhs
        operation: FirOperation,
        convert: T.() -> FirExpression
    ): FirStatement {
        val tokenType = this?.elementType
        if (tokenType == PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(baseSource, rhs, value, operation, convert)
        }
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
            require(this != null)
            if (operation == FirOperation.ASSIGN) {
                context.arraySetArgument[this] = value
            }
            return if (operation == FirOperation.ASSIGN) {
                this.convert()
            } else {
                generateAugmentedArraySetCall(baseSource, operation, rhs, convert)
            }
        }

        if (operation in FirOperation.ASSIGNMENTS && operation != FirOperation.ASSIGN) {
            return buildAssignmentOperatorStatement {
                source = baseSource
                this.operation = operation
                // TODO: take good psi
                leftArgument = this@generateAssignment?.convert() ?: buildErrorExpression {
                    source = null
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.ExpressionRequired
                    )
                }
                rightArgument = value
            }
        }
        require(operation == FirOperation.ASSIGN)

        if (this?.elementType == SAFE_ACCESS_EXPRESSION && this != null) {
            val safeCallNonAssignment = convert() as? FirSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, value)
            }
        }

        return buildVariableAssignment {
            source = baseSource
            rValue = value
            calleeReference = initializeLValue(this@generateAssignment) { convert() as? FirQualifiedAccess }
        }
    }

    // gets a?.{ $subj.x } and turns it to a?.{ $subj.x = v }
    private fun putAssignmentToSafeCall(
        safeCallNonAssignment: FirSafeCallExpression,
        baseSource: FirSourceElement?,
        value: FirExpression
    ): FirSafeCallExpression {
        val nestedAccess = safeCallNonAssignment.regularQualifiedAccess

        val assignment = buildVariableAssignment {
            source = baseSource
            rValue = value
            calleeReference = nestedAccess.calleeReference
            explicitReceiver = safeCallNonAssignment.checkedSubjectRef.value
        }

        safeCallNonAssignment.replaceRegularQualifiedAccess(
            assignment
        )

        return safeCallNonAssignment
    }

    private fun T.generateAugmentedArraySetCall(
        baseSource: FirSourceElement?,
        operation: FirOperation,
        rhs: T?,
        convert: T.() -> FirExpression
    ): FirStatement {
        return buildAugmentedArraySetCall {
            source = baseSource
            this.operation = operation
            assignCall = generateAugmentedCallForAugmentedArraySetCall(operation, rhs, convert)
            setGetBlock = generateSetGetBlockForAugmentedArraySetCall(baseSource, operation, rhs, convert)
        }
    }

    private fun T.generateAugmentedCallForAugmentedArraySetCall(
        operation: FirOperation,
        rhs: T?,
        convert: T.() -> FirExpression
    ): FirFunctionCall {
        /*
         * Desugarings of a[x, y] += z to
         * a.get(x, y).plusAssign(z)
         */
        return buildFunctionCall {
            calleeReference = buildSimpleNamedReference {
                name = FirOperationNameConventions.ASSIGNMENTS.getValue(operation)
            }
            explicitReceiver = convert()
            argumentList = buildArgumentList {
                arguments += rhs?.convert() ?: buildErrorExpression(
                    null,
                    ConeSimpleDiagnostic("No value for array set", DiagnosticKind.Syntax)
                )
            }
        }
    }


    private fun T.generateSetGetBlockForAugmentedArraySetCall(
        baseSource: FirSourceElement?,
        operation: FirOperation,
        rhs: T?,
        convert: T.() -> FirExpression
    ): FirBlock {
        /*
         * Desugarings of a[x, y] += z to
         * {
         *     val tmp_a = a
         *     val tmp_x = x
         *     val tmp_y = y
         *     tmp_a.set(tmp_x, tmp_a.get(tmp_x, tmp_y).plus(z))
         * }
         */
        return buildBlock {
            val baseCall = convert() as FirFunctionCall

            val arrayVariable = generateTemporaryVariable(
                baseSession,
                source = null,
                "<array>",
                baseCall.explicitReceiver ?: buildErrorExpression {
                    source = baseSource
                    diagnostic = ConeSimpleDiagnostic("No receiver for array access", DiagnosticKind.Syntax)
                }
            )
            statements += arrayVariable
            val indexVariables = baseCall.arguments.mapIndexed { i, index ->
                generateTemporaryVariable(baseSession, source = null, "<index_$i>", index)
            }
            statements += indexVariables
            statements += buildFunctionCall {
                source = baseSource
                explicitReceiver = arrayVariable.toQualifiedAccess()
                calleeReference = buildSimpleNamedReference {
                    name = OperatorNameConventions.SET
                }
                argumentList = buildArgumentList {
                    for (indexVariable in indexVariables) {
                        arguments += indexVariable.toQualifiedAccess()
                    }

                    val getCall = buildFunctionCall {
                        explicitReceiver = arrayVariable.toQualifiedAccess()
                        calleeReference = buildSimpleNamedReference {
                            name = OperatorNameConventions.GET
                        }
                        argumentList = buildArgumentList {
                            for (indexVariable in indexVariables) {
                                arguments += indexVariable.toQualifiedAccess()
                            }
                        }
                    }

                    val operatorCall = buildFunctionCall {
                        calleeReference = buildSimpleNamedReference {
                            name = FirOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operation)
                        }
                        explicitReceiver = getCall
                        argumentList = buildArgumentList {
                            arguments += rhs?.convert() ?: buildErrorExpression(
                                null,
                                ConeSimpleDiagnostic(
                                    "No value for array set",
                                    DiagnosticKind.Syntax
                                )
                            )
                        }
                    }
                    arguments += operatorCall
                }
            }
        }
    }

    inner class DataClassMembersGenerator(
        private val session: FirSession,
        private val source: T,
        private val classBuilder: FirRegularClassBuilder,
        private val zippedParameters: List<Pair<T, FirProperty>>,
        private val packageFqName: FqName,
        private val classFqName: FqName,
        private val createClassTypeRefWithSourceKind: (FirFakeSourceElementKind) -> FirTypeRef,
        private val createParameterTypeRefWithSourceKind: (FirProperty, FirFakeSourceElementKind) -> FirTypeRef,
    ) {
        fun generate() {
            generateComponentFunctions()
            generateCopyFunction()
            // Refer to (IR utils or FIR backend) DataClassMembersGenerator for generating equals, hashCode, and toString
        }

        private fun generateComponentAccess(
            parameterSource: FirSourceElement?,
            firProperty: FirProperty,
            classTypeRefWithCorrectSourceKind: FirTypeRef,
            firPropertyReturnTypeRefWithCorrectSourceKind: FirTypeRef
        ) =
            buildQualifiedAccessExpression {
                source = parameterSource
                typeRef = firPropertyReturnTypeRefWithCorrectSourceKind
                dispatchReceiver = buildThisReceiverExpression {
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
                    source = parameterSource?.fakeElement(FirFakeSourceElementKind.DataClassGeneratedMembers)
                    session = this@DataClassMembersGenerator.session
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = firProperty.returnTypeRef
                    receiverTypeRef = null
                    this.name = name
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                    symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))

                    // Refer to FIR backend ClassMemberGenerator for body generation.
                }
                classBuilder.addDeclaration(componentFunction)
            }
        }

        private val copyName = Name.identifier("copy")

        private fun generateCopyFunction() {
            classBuilder.addDeclaration(
                buildSimpleFunction {
                    val classTypeRef = createClassTypeRefWithSourceKind(FirFakeSourceElementKind.DataClassGeneratedMembers)
                    source = this@DataClassMembersGenerator.source.toFirSourceElement(FirFakeSourceElementKind.DataClassGeneratedMembers)
                    session = this@DataClassMembersGenerator.session
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = classTypeRef
                    name = copyName
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                    symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, copyName))
                    for ((ktParameter, firProperty) in zippedParameters) {
                        val propertyName = firProperty.name
                        val parameterSource = ktParameter?.toFirSourceElement(FirFakeSourceElementKind.DataClassGeneratedMembers)
                        val propertyReturnTypeRef =
                            createParameterTypeRefWithSourceKind(firProperty, FirFakeSourceElementKind.DataClassGeneratedMembers)
                        valueParameters += buildValueParameter {
                            source = parameterSource
                            session = this@DataClassMembersGenerator.session
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = propertyReturnTypeRef
                            name = propertyName
                            symbol = FirVariableSymbol(propertyName)
                            defaultValue = generateComponentAccess(parameterSource, firProperty, propertyReturnTypeRef, classTypeRef)
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

    private fun FirVariable<*>.toQualifiedAccess(): FirQualifiedAccessExpression = buildQualifiedAccessExpression {
        calleeReference = buildResolvedNamedReference {
            name = this@toQualifiedAccess.name
            resolvedSymbol = this@toQualifiedAccess.symbol
        }
    }

    protected inline fun <R> withDefaultSourceElementKind(newDefault: FirSourceElementKind, action: () -> R): R {
        val currentForced = context.forcedElementSourceKind
        context.forcedElementSourceKind = newDefault
        try {
            return action()
        } finally {
            context.forcedElementSourceKind = currentForced
        }
    }
}
