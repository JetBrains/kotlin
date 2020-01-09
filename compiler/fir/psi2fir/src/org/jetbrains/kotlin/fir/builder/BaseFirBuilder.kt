/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorFunctionImpl
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
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
abstract class BaseFirBuilder<T>(val session: FirSession, val context: Context = Context()) {

    protected val implicitUnitType = session.builtinTypes.unitType
    protected val implicitAnyType = session.builtinTypes.anyType
    protected val implicitEnumType = session.builtinTypes.enumType
    protected val implicitAnnotationType = session.builtinTypes.annotationType

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
        val t = l()
        context.className = context.className.parent()
        return t
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
        return FirReturnExpressionImpl(
            baseSource,
            this
        ).apply {
            target = FirFunctionTarget(labelName)
            if (labelName == null) {
                val lastFunction = context.firFunctions.lastOrNull { !(it is FirAnonymousFunction && it.isLambda) }
                if (lastFunction != null) {
                    target.bind(lastFunction)
                } else {
                    target.bind(
                        FirErrorFunctionImpl(
                            source,
                            this@BaseFirBuilder.session,
                            FirSimpleDiagnostic("Cannot bind unlabeled return to a function", DiagnosticKind.ReturnNotAllowed),
                            FirErrorFunctionSymbol()
                        )
                    )
                }
            } else {
                for (firFunction in context.firFunctions.asReversed()) {
                    when (firFunction) {
                        is FirAnonymousFunction -> {
                            if (firFunction.label?.name == labelName) {
                                target.bind(firFunction)
                                return@apply
                            }
                        }
                        is FirMemberFunction<*> -> {
                            if (firFunction.name.asString() == labelName) {
                                target.bind(firFunction)
                                return@apply
                            }
                        }
                    }
                }
                target.bind(
                    FirErrorFunctionImpl(
                        source,
                        this@BaseFirBuilder.session,
                        FirSimpleDiagnostic("Cannot bind label $labelName to a function", DiagnosticKind.UnresolvedLabel),
                        FirErrorFunctionSymbol()
                    )
                )
            }
        }
    }

    fun KtClassOrObject?.toDelegatedSelfType(firClass: FirRegularClass): FirResolvedTypeRef {
        return FirResolvedTypeRefImpl(
            this?.toFirSourceElement(),
            ConeClassLikeTypeImpl(
                firClass.symbol.toLookupTag(),
                firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        )
    }

    fun typeParametersFromSelfType(delegatedSelfTypeRef: FirTypeRef): List<FirTypeParameter> {
        return delegatedSelfTypeRef.coneTypeSafe<ConeKotlinType>()
            ?.typeArguments
            ?.map { ((it as ConeTypeParameterType).lookupTag.symbol as FirTypeParameterSymbol).fir }
            ?: emptyList()
    }

    fun FirAbstractLoop.configure(generateBlock: () -> FirBlock): FirAbstractLoop {
        label = context.firLabels.pop()
        context.firLoops += this
        block = generateBlock()
        context.firLoops.removeLast()
        return this
    }

    fun FirAbstractLoopJump.bindLabel(expression: T): FirAbstractLoopJump {
        val labelName = expression.getLabelName()
        target = FirLoopTarget(labelName)
        val lastLoop = context.firLoops.lastOrNull()
        if (labelName == null) {
            if (lastLoop != null) {
                target.bind(lastLoop)
            } else {
                target.bind(
                    FirErrorLoop(
                        expression.getSourceOrNull(), FirSimpleDiagnostic("Cannot bind unlabeled jump to a loop", DiagnosticKind.Syntax)
                    )
                )
            }
        } else {
            for (firLoop in context.firLoops.asReversed()) {
                if (firLoop.label?.name == labelName) {
                    target.bind(firLoop)
                    return this
                }
            }
            target.bind(
                FirErrorLoop(
                    expression.getSourceOrNull(), FirSimpleDiagnostic("Cannot bind label $labelName to a loop", DiagnosticKind.Syntax)
                )
            )
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
                    convertedText !is Long -> return FirErrorExpressionImpl(
                        expression.getSourceOrNull(),
                        FirSimpleDiagnostic(
                            "Incorrect constant expression: $text",
                            DiagnosticKind.IllegalConstExpression
                        )
                    )

                    hasLongSuffix(text) || hasUnsignedSuffix(text) || hasUnsignedLongSuffix(text) -> {
                        FirConstKind.Long
                    }

                    else -> {
                        FirConstKind.IntegerLiteral
                    }
                }

                FirConstExpressionImpl(
                    expression.getSourceOrNull(),
                    kind,
                    convertedText,
                    FirSimpleDiagnostic("Incorrect integer literal: $text", DiagnosticKind.Syntax)
                )
            }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    FirConstExpressionImpl(
                        expression.getSourceOrNull(), FirConstKind.Float, convertedText,
                        FirSimpleDiagnostic("Incorrect float: $text", DiagnosticKind.Syntax)
                    )
                } else {
                    FirConstExpressionImpl(
                        expression.getSourceOrNull(), FirConstKind.Double, convertedText as Double,
                        FirSimpleDiagnostic("Incorrect double: $text", DiagnosticKind.Syntax)
                    )
                }
            CHARACTER_CONSTANT ->
                FirConstExpressionImpl(
                    expression.getSourceOrNull(), FirConstKind.Char, text.parseCharacter(),
                    FirSimpleDiagnostic("Incorrect character: $text", DiagnosticKind.Syntax)
                )
            BOOLEAN_CONSTANT ->
                FirConstExpressionImpl(expression.getSourceOrNull(), FirConstKind.Boolean, convertedText as Boolean)
            NULL ->
                FirConstExpressionImpl(expression.getSourceOrNull(), FirConstKind.Null, null)
            else ->
                throw AssertionError("Unknown literal type: $type, $text")
        }
    }

    fun Array<out T?>.toInterpolatingCall(
        base: KtStringTemplateExpression?,
        convertTemplateEntry: T?.(String) -> FirExpression
    ): FirExpression {
        val sb = StringBuilder()
        var hasExpressions = false
        var result: FirExpression? = null
        var callCreated = false
        L@ for (entry in this) {
            if (entry == null) continue
            val nextArgument = when (entry.elementType) {
                OPEN_QUOTE, CLOSING_QUOTE -> continue@L
                LITERAL_STRING_TEMPLATE_ENTRY -> {
                    sb.append(entry.asText)
                    FirConstExpressionImpl(entry.getSourceOrNull(), FirConstKind.String, entry.asText)
                }
                ESCAPE_STRING_TEMPLATE_ENTRY -> {
                    sb.append(entry.unescapedValue)
                    FirConstExpressionImpl(entry.getSourceOrNull(), FirConstKind.String, entry.unescapedValue)
                }
                SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                    hasExpressions = true
                    val firExpression = entry.convertTemplateEntry("Incorrect template argument")
                    val source = firExpression.source
                    FirFunctionCallImpl(source).apply {
                        explicitReceiver = firExpression
                        calleeReference = FirSimpleNamedReference(source, Name.identifier("toString"), candidateSymbol = null)
                    }
                }
                else -> {
                    hasExpressions = true
                    FirErrorExpressionImpl(
                        entry.getSourceOrNull(), FirSimpleDiagnostic("Incorrect template entry: ${entry.asText}", DiagnosticKind.Syntax)
                    )
                }
            }
            result = when {
                result == null -> nextArgument
                callCreated && result is FirStringConcatenationCallImpl -> result.apply {
                    arguments += nextArgument
                }
                else -> {
                    callCreated = true
                    FirStringConcatenationCallImpl(base?.toFirSourceElement()).apply {
                        arguments += result!!
                        arguments += nextArgument
                    }
                }
            }
        }
        return if (hasExpressions) result!! else FirConstExpressionImpl(base?.toFirSourceElement(), FirConstKind.String, sb.toString())
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
            return FirErrorExpressionImpl(argument, FirSimpleDiagnostic("Inc/dec without operand", DiagnosticKind.Syntax))
        }
        val source = baseExpression?.toFirSourceElement()
        return FirBlockImpl(source).apply {
            val tempName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(this@BaseFirBuilder.session, source, tempName, argument.convert())
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = FirFunctionCallImpl(source).apply {
                this.calleeReference = FirSimpleNamedReference(baseExpression?.operationReference?.toFirSourceElement(), callName, null)
                this.explicitReceiver = generateResolvedAccessExpression(source, temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(this@BaseFirBuilder.session, source, resultName, resultInitializer)
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

    private fun FirModifiableQualifiedAccess.initializeLValue(
        left: T?,
        convertQualified: T.() -> FirQualifiedAccess?
    ): FirReference {
        val tokenType = left?.elementType
        if (left != null) {
            when (tokenType) {
                REFERENCE_EXPRESSION -> {
                    return FirSimpleNamedReference(left.getSourceOrNull(), left.getReferencedNameAsName(), null)
                }
                THIS_EXPRESSION -> {
                    return FirExplicitThisReference(left.getSourceOrNull(), left.getLabelName())
                }
                DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> {
                    val firMemberAccess = left.convertQualified()
                    return if (firMemberAccess != null) {
                        explicitReceiver = firMemberAccess.explicitReceiver
                        safe = firMemberAccess.safe
                        firMemberAccess.calleeReference
                    } else {
                        FirErrorNamedReferenceImpl(
                            left.getSourceOrNull(),
                            FirSimpleDiagnostic("Unsupported qualified LValue: ${left.asText}", DiagnosticKind.Syntax)
                        )
                    }
                }
                PARENTHESIZED -> {
                    return initializeLValue(left.getExpressionInParentheses(), convertQualified)
                }
            }
        }
        return FirErrorNamedReferenceImpl(
            left.getSourceOrNull(), FirSimpleDiagnostic("Unsupported LValue: $tokenType", DiagnosticKind.Syntax)
        )
    }

    fun T?.generateAssignment(
        source: FirSourceElement?,
        value: FirExpression,
        operation: FirOperation,
        convert: T.() -> FirExpression
    ): FirStatement {
        val tokenType = this?.elementType
        if (tokenType == PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(source, value, operation, convert)
        }
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
            val firArrayAccess = this!!.convert() as FirFunctionCallImpl
            val arraySet = if (operation != FirOperation.ASSIGN) {
                FirArraySetCallImpl(source, value, operation).apply {
                    indexes += firArrayAccess.arguments
                }
            } else {
                return firArrayAccess.apply {
                    calleeReference = FirSimpleNamedReference(source, OperatorNameConventions.SET, null)
                    arguments += value
                }
            }
            val arrayExpression = this.getChildNodeByType(REFERENCE_EXPRESSION)
            if (arrayExpression != null) {
                return arraySet.apply {
                    lValue = initializeLValue(arrayExpression) { convert() as? FirQualifiedAccess }
                }
            }
            val psiArrayExpression = firArrayAccess.explicitReceiver?.psi
            return FirBlockImpl(psiArrayExpression?.toFirSourceElement()).apply {
                val name = Name.special("<array-set>")
                statements += generateTemporaryVariable(
                    this@BaseFirBuilder.session, this@generateAssignment.getSourceOrNull(), name, firArrayAccess.explicitReceiver!!
                )
                statements += arraySet.apply { lValue = FirSimpleNamedReference(psiArrayExpression?.toFirSourceElement(), name, null) }
            }
        }

        if (operation in FirOperation.ASSIGNMENTS && operation != FirOperation.ASSIGN) {
            return FirOperatorCallImpl(source, operation).apply {
                // TODO: take good psi
                arguments += this@generateAssignment?.convert() ?:
                        FirErrorExpressionImpl(
                            null, FirSimpleDiagnostic("Unsupported left value of assignment: ${source?.psi?.text}", DiagnosticKind.Syntax)
                        )
                arguments += value
            }
        }
        require(operation == FirOperation.ASSIGN)
        return FirVariableAssignmentImpl(source, false, value).apply {
            lValue = initializeLValue(this@generateAssignment) { convert() as? FirQualifiedAccess }
        }
    }
}
