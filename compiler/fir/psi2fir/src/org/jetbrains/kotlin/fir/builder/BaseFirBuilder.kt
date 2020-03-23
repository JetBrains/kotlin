/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens.CLOSING_QUOTE
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_QUOTE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.util.OperatorNameConventions

//T can be either PsiElement, or LighterASTNode
abstract class BaseFirBuilder<T>(val baseSession: FirSession, val context: Context<T> = Context()) {

    abstract fun T.toFirSourceElement(): FirSourceElement

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
    abstract fun T.getChildNodeByType(type: IElementType): T?
    abstract val T?.selectorExpression: T?

    /**** Class name utils ****/
    inline fun <T> withChildClassName(name: Name, l: () -> T): T {
        context.className = context.className.child(name)
        return try {
            l()
        } finally {
            context.className = context.className.parent()
        }
    }

    fun callableIdForName(name: Name, local: Boolean = false) =
        when {
            local -> CallableId(name)
            context.className == FqName.ROOT -> CallableId(context.packageFqName, name)
            context.className.shortName() === ANONYMOUS_OBJECT_NAME -> CallableId(FqName.ROOT, FqName("anonymous"), name)
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
                        diagnostic = ConeSimpleDiagnostic(message, kind)
                        symbol = FirErrorFunctionSymbol()
                    }
                )
            }

            source = baseSource
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

    fun T?.toDelegatedSelfType(firClass: AbstractFirRegularClassBuilder): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = this@toDelegatedSelfType?.toFirSourceElement()
            type = ConeClassLikeTypeImpl(
                firClass.symbol.toLookupTag(),
                firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }
    }

    fun T?.toDelegatedSelfType(firObject: FirAnonymousObjectBuilder): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = this@toDelegatedSelfType?.toFirSourceElement()
            type = ConeClassLikeTypeImpl(firObject.symbol.toLookupTag(), emptyArray(), false)
        }
    }

    fun typeParametersFromSelfType(delegatedSelfTypeRef: FirTypeRef): List<FirTypeParameter> {
        return delegatedSelfTypeRef.coneTypeSafe<ConeKotlinType>()
            ?.typeArguments
            ?.map { ((it as ConeTypeParameterType).lookupTag.symbol as FirTypeParameterSymbol).fir }
            ?: emptyList()
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
        if (labelName == null) {
            target = lastLoopTarget ?: FirLoopTarget(labelName).apply {
                bind(
                    buildErrorLoop(
                        expression.getSourceOrNull(),
                        ConeSimpleDiagnostic("Cannot bind unlabeled jump to a loop", DiagnosticKind.Syntax)
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
                        expression.getSourceOrNull(), ConeSimpleDiagnostic("Cannot bind label $labelName to a loop", DiagnosticKind.Syntax)
                    )
                )
            }
        }
        return this
    }

    /**** Conversion utils ****/
    private fun <T> T.getSourceOrNull(): FirSourceElement? {
        return if (this is PsiElement) FirPsiSourceElement(this) else null
    }

    fun generateConstantExpressionByLiteral(expression: T): FirExpression {
        val type = expression.elementType
        val text: String = expression.asText
        val convertedText: Any? = when (type) {
            INTEGER_CONSTANT, FLOAT_CONSTANT -> parseNumericLiteral(text, type)
            BOOLEAN_CONSTANT -> parseBoolean(text)
            else -> null
        }
        return when (type) {
            INTEGER_CONSTANT -> {
                val kind = when {
                    convertedText !is Long -> return buildErrorExpression {
                        source = expression.getSourceOrNull()
                        diagnostic = ConeSimpleDiagnostic(
                            "Incorrect constant expression: $text",
                            DiagnosticKind.IllegalConstExpression
                        )
                    }

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
                    expression.getSourceOrNull(),
                    kind,
                    convertedText,
                    ConeSimpleDiagnostic("Incorrect integer literal: $text", DiagnosticKind.Syntax)
                )
            }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    buildConstOrErrorExpression(
                        expression.getSourceOrNull(),
                        FirConstKind.Float,
                        convertedText,
                        ConeSimpleDiagnostic("Incorrect float: $text", DiagnosticKind.Syntax)
                    )
                } else {
                    buildConstOrErrorExpression(
                        expression.getSourceOrNull(),
                        FirConstKind.Double,
                        convertedText as Double,
                        ConeSimpleDiagnostic("Incorrect double: $text", DiagnosticKind.Syntax)
                    )
                }
            CHARACTER_CONSTANT ->
                buildConstOrErrorExpression(
                    expression.getSourceOrNull(),
                    FirConstKind.Char,
                    text.parseCharacter(),
                    ConeSimpleDiagnostic("Incorrect character: $text", DiagnosticKind.Syntax)
                )
            BOOLEAN_CONSTANT ->
                buildConstExpression(
                    expression.getSourceOrNull(),
                    FirConstKind.Boolean,
                    convertedText as Boolean
                )
            NULL ->
                buildConstExpression(
                    expression.getSourceOrNull(),
                    FirConstKind.Null,
                    null
                )
            else ->
                throw AssertionError("Unknown literal type: $type, $text")
        }
    }

    fun Array<out T?>.toInterpolatingCall(
        base: KtStringTemplateExpression?,
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
                            buildConstExpression(entry.getSourceOrNull(), FirConstKind.String, entry.asText)
                        }
                        ESCAPE_STRING_TEMPLATE_ENTRY -> {
                            sb.append(entry.unescapedValue)
                            buildConstExpression(entry.getSourceOrNull(), FirConstKind.String, entry.unescapedValue)
                        }
                        SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                            hasExpressions = true
                            val firExpression = entry.convertTemplateEntry("Incorrect template argument")
                            val source = firExpression.source
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
                                source = entry.getSourceOrNull()
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
        baseExpression: KtUnaryExpression?,
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
            source = baseSource
            val tempName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(this@BaseFirBuilder.baseSession, source, tempName, argument.convert())
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = buildFunctionCall {
                source = baseSource
                calleeReference = buildSimpleNamedReference {
                    source = baseExpression?.operationReference?.toFirSourceElement()
                    name = callName
                }
                explicitReceiver = generateResolvedAccessExpression(source, temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(this@BaseFirBuilder.baseSession, source, resultName, resultInitializer)
            val assignment = argument.generateAssignment(
                source,
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
                    statements += generateResolvedAccessExpression(source, resultVar)
                } else {
                    appendAssignment()
                    statements += generateAccessExpression(source, argument.getReferencedNameAsName())
                }
            } else {
                appendAssignment()
                statements += generateResolvedAccessExpression(source, temporaryVariable)
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
                        source = left.getSourceOrNull()
                        name = left.getReferencedNameAsName()
                    }
                }
                THIS_EXPRESSION -> {
                    return buildExplicitThisReference {
                        source = left.getSourceOrNull()
                        labelName = left.getLabelName()
                    }
                }
                DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> {
                    val firMemberAccess = left.convertQualified()
                    return if (firMemberAccess != null) {
                        explicitReceiver = firMemberAccess.explicitReceiver
                        safe = firMemberAccess.safe
                        firMemberAccess.calleeReference
                    } else {
                        buildErrorNamedReference {
                            source = left.getSourceOrNull()
                            diagnostic = ConeSimpleDiagnostic("Unsupported qualified LValue: ${left.asText}", DiagnosticKind.Syntax)
                        }
                    }
                }
                PARENTHESIZED -> {
                    return initializeLValue(left.getExpressionInParentheses(), convertQualified)
                }
            }
        }
        return buildErrorNamedReference {
            source = left.getSourceOrNull()
            diagnostic = ConeSimpleDiagnostic("Unsupported LValue: $tokenType", DiagnosticKind.Syntax)
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
            return buildOperatorCall {
                source = baseSource
                this.operation = operation
                // TODO: take good psi
                argumentList = buildBinaryArgumentList(
                    this@generateAssignment?.convert() ?: buildErrorExpression {
                        source = null
                        diagnostic = ConeSimpleDiagnostic("Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.Syntax)
                    },
                    value
                )
            }
        }
        require(operation == FirOperation.ASSIGN)
        return buildVariableAssignment {
            source = baseSource
            safe = false
            rValue = value
            calleeReference = initializeLValue(this@generateAssignment) { convert() as? FirQualifiedAccess }
        }
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

    fun List<Pair<T, FirProperty>>.generateComponentFunctions(
        session: FirSession, firClassBuilder: AbstractFirRegularClassBuilder, packageFqName: FqName, classFqName: FqName,
        firPrimaryConstructor: FirConstructor,
    ) {
        var componentIndex = 1
        for ((sourceNode, firProperty) in this) {
            if (!firProperty.isVal && !firProperty.isVar) continue
            val name = Name.identifier("component$componentIndex")
            componentIndex++
            val parameterSource = sourceNode?.toFirSourceElement()
            val target = FirFunctionTarget(labelName = null, isLambda = false)
            val componentFunction = buildSimpleFunction {
                source = parameterSource
                this.session = session
                returnTypeRef = buildImplicitTypeRef {
                    source = parameterSource
                }
                receiverTypeRef = null
                this.name = name
                this.status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL)
                this.symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))

                val returnExpression = buildReturnExpression {
                    source = parameterSource
                    result = buildQualifiedAccessExpression {
                        source = parameterSource
                        dispatchReceiver = buildThisReceiverExpression {
                            calleeReference = buildImplicitThisReference {
                                boundSymbol = firClassBuilder.symbol
                            }
                            typeRef = firPrimaryConstructor.returnTypeRef
                        }
                        calleeReference = buildResolvedNamedReference {
                            source = parameterSource
                            this.name = firProperty.name
                            resolvedSymbol = firProperty.symbol
                        }
                    }
                    this.target = target
                }
                body = buildSingleExpressionBlock(returnExpression)
            }.also {
                target.bind(it)
            }
            firClassBuilder.addDeclaration(componentFunction)
        }
    }

    private val copyName = Name.identifier("copy")

    fun List<Pair<T, FirProperty>>.generateCopyFunction(
        session: FirSession,
        classOrObject: KtClassOrObject?,
        classBuilder: AbstractFirRegularClassBuilder,
        packageFqName: FqName,
        classFqName: FqName,
        firPrimaryConstructor: FirConstructor,
    ) {
        classBuilder.addDeclaration(
            buildSimpleFunction {
                source = classOrObject?.toFirSourceElement()
                this.session = session
                returnTypeRef = firPrimaryConstructor.returnTypeRef
                name = copyName
                status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL)
                symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, copyName))
                for ((ktParameter, firProperty) in this@generateCopyFunction) {
                    val propertyName = firProperty.name
                    val parameterSource = ktParameter?.toFirSourceElement()
                    valueParameters += buildValueParameter {
                        source = parameterSource
                        this.session = session
                        returnTypeRef = firProperty.returnTypeRef
                        name = propertyName
                        symbol = FirVariableSymbol(propertyName)
                        defaultValue = buildQualifiedAccessExpression {
                            source = parameterSource
                            dispatchReceiver = buildThisReceiverExpression {
                                calleeReference = buildImplicitThisReference {
                                    boundSymbol = classBuilder.symbol
                                }
                                typeRef = firPrimaryConstructor.returnTypeRef
                            }
                            calleeReference = buildResolvedNamedReference {
                                source = parameterSource
                                this.name = propertyName
                                resolvedSymbol = firProperty.symbol
                            }
                        }
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                }

                body = buildEmptyExpressionBlock()
            },
        )
    }

    private fun FirVariable<*>.toQualifiedAccess(): FirQualifiedAccessExpression = buildQualifiedAccessExpression {
        calleeReference = buildResolvedNamedReference {
            name = this@toQualifiedAccess.name
            resolvedSymbol = this@toQualifiedAccess.symbol
        }
    }
}
