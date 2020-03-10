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
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
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
                        diagnostic = FirSimpleDiagnostic(message, kind)
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
                        FirSimpleDiagnostic("Cannot bind unlabeled jump to a loop", DiagnosticKind.Syntax)
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
                        expression.getSourceOrNull(), FirSimpleDiagnostic("Cannot bind label $labelName to a loop", DiagnosticKind.Syntax)
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
                        diagnostic = FirSimpleDiagnostic(
                            "Incorrect constant expression: $text",
                            DiagnosticKind.IllegalConstExpression
                        )
                    }

                    hasLongSuffix(text) || hasUnsignedSuffix(text) || hasUnsignedLongSuffix(text) -> {
                        FirConstKind.Long
                    }

                    else -> {
                        FirConstKind.IntegerLiteral
                    }
                }

                buildConstOrErrorExpression(
                    expression.getSourceOrNull(),
                    kind,
                    convertedText,
                    FirSimpleDiagnostic("Incorrect integer literal: $text", DiagnosticKind.Syntax)
                )
            }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    buildConstOrErrorExpression(
                        expression.getSourceOrNull(),
                        FirConstKind.Float,
                        convertedText,
                        FirSimpleDiagnostic("Incorrect float: $text", DiagnosticKind.Syntax)
                    )
                } else {
                    buildConstOrErrorExpression(
                        expression.getSourceOrNull(),
                        FirConstKind.Double,
                        convertedText as Double,
                        FirSimpleDiagnostic("Incorrect double: $text", DiagnosticKind.Syntax)
                    )
                }
            CHARACTER_CONSTANT ->
                buildConstOrErrorExpression(
                    expression.getSourceOrNull(),
                    FirConstKind.Char,
                    text.parseCharacter(),
                    FirSimpleDiagnostic("Incorrect character: $text", DiagnosticKind.Syntax)
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
                            diagnostic = FirSimpleDiagnostic("Incorrect template entry: ${entry.asText}", DiagnosticKind.Syntax)
                        }
                    }
                }
            }
            source = base?.toFirSourceElement()
            // Fast-pass if there is no non-const string expressions
            if (!hasExpressions) return buildConstExpression(source, FirConstKind.String, sb.toString())
            arguments.singleOrNull()?.let { return it }
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
                diagnostic = FirSimpleDiagnostic("Inc/dec without operand", DiagnosticKind.Syntax)
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
                            diagnostic = FirSimpleDiagnostic("Unsupported qualified LValue: ${left.asText}", DiagnosticKind.Syntax)
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
            diagnostic = FirSimpleDiagnostic("Unsupported LValue: $tokenType", DiagnosticKind.Syntax)
        }
    }

    fun T?.generateAssignment(
        baseSource: FirSourceElement?,
        value: FirExpression,
        operation: FirOperation,
        convert: T.() -> FirExpression
    ): FirStatement {
        val tokenType = this?.elementType
        if (tokenType == PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(baseSource, value, operation, convert)
        }
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
            if (operation == FirOperation.ASSIGN) {
                context.arraySetArgument[this!!] = value
            }
            val firArrayAccess = this!!.convert() as FirFunctionCall
            if (operation == FirOperation.ASSIGN) {
                return firArrayAccess
            }
            val arraySetCallBuilder = FirArraySetCallBuilder().apply {
                source = baseSource
                rValue = value
                this.operation = operation
                indexes += firArrayAccess.arguments
            }
            val arrayExpression = this.getChildNodeByType(REFERENCE_EXPRESSION)
            if (arrayExpression != null) {
                return arraySetCallBuilder.apply {
                    calleeReference = initializeLValue(arrayExpression) { convert() as? FirQualifiedAccess }
                }.build()
            }
            val psiArrayExpression = firArrayAccess.explicitReceiver?.psi
            return buildBlock {
                source = psiArrayExpression?.toFirSourceElement()
                val name = Name.special("<array-set>")
                statements += generateTemporaryVariable(
                    this@BaseFirBuilder.baseSession, this@generateAssignment.getSourceOrNull(), name, firArrayAccess.explicitReceiver!!
                )
                statements += arraySetCallBuilder.apply {
                    calleeReference = buildSimpleNamedReference {
                        source = psiArrayExpression?.toFirSourceElement()
                        this.name = name
                    }
                }.build()
            }
        }

        if (operation in FirOperation.ASSIGNMENTS && operation != FirOperation.ASSIGN) {
            return buildOperatorCall {
                source = baseSource
                this.operation = operation
                // TODO: take good psi
                arguments += this@generateAssignment?.convert() ?:
                        buildErrorExpression {
                            source = null
                            diagnostic = FirSimpleDiagnostic("Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.Syntax)
                        }
                arguments += value
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

}
