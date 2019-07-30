/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.lexer.KtTokens.CLOSING_QUOTE
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_QUOTE
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

//T can be either PsiElement, or LighterASTNode
abstract class BaseFirBuilder<T>(val session: FirSession, val context: Context = Context()) {

    protected val implicitUnitType = FirImplicitUnitTypeRef(session, null)
    protected val implicitAnyType = FirImplicitAnyTypeRef(session, null)
    protected val implicitEnumType = FirImplicitEnumTypeRef(session, null)
    protected val implicitAnnotationType = FirImplicitAnnotationTypeRef(session, null)

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
    fun FirExpression.toReturn(basePsi: PsiElement? = psi, labelName: String? = null): FirReturnExpression {
        return FirReturnExpressionImpl(
            this@BaseFirBuilder.session,
            basePsi,
            this
        ).apply {
            target = FirFunctionTarget(labelName)
            val lastFunction = context.firFunctions.lastOrNull()
            if (labelName == null) {
                if (lastFunction != null) {
                    target.bind(lastFunction)
                } else {
                    target.bind(FirErrorFunction(this@BaseFirBuilder.session, psi, "Cannot bind unlabeled return to a function"))
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
                        is FirNamedFunction -> {
                            if (firFunction.name.asString() == labelName) {
                                target.bind(firFunction)
                                return@apply
                            }
                        }
                    }
                }
                target.bind(FirErrorFunction(this@BaseFirBuilder.session, psi, "Cannot bind label $labelName to a function"))
            }
        }
    }

    fun KtClassOrObject?.toDelegatedSelfType(firClass: FirRegularClass): FirTypeRef {
        val typeParameters = firClass.typeParameters.map {
            FirTypeParameterImpl(session, it.psi, FirTypeParameterSymbol(), it.name, Variance.INVARIANT, false).apply {
                this.bounds += it.bounds
            }
        }
        return FirResolvedTypeRefImpl(
            session,
            this,
            ConeClassTypeImpl(
                firClass.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
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

    /**** Conversion utils ****/
    private fun <T> T.getPsiOrNull(): PsiElement? {
        return if (this is PsiElement) this else null
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
            INTEGER_CONSTANT ->
                if (convertedText is Long &&
                    (hasLongSuffix(text) || hasUnsignedLongSuffix(text) || hasUnsignedSuffix(text) ||
                            convertedText > Int.MAX_VALUE || convertedText < Int.MIN_VALUE)
                ) {
                    FirConstExpressionImpl(
                        session, expression.getPsiOrNull(), IrConstKind.Long, convertedText, "Incorrect long: $text"
                    )
                } else if (convertedText is Number) {
                    // TODO: support byte / short
                    FirConstExpressionImpl(session, expression.getPsiOrNull(), IrConstKind.Int, convertedText.toInt(), "Incorrect int: $text")
                } else {
                    FirErrorExpressionImpl(session, expression.getPsiOrNull(), reason = "Incorrect constant expression: $text")
                }
            FLOAT_CONSTANT ->
                if (convertedText is Float) {
                    FirConstExpressionImpl(
                        session, expression.getPsiOrNull(), IrConstKind.Float, convertedText, "Incorrect float: $text"
                    )
                } else {
                    FirConstExpressionImpl(
                        session, expression.getPsiOrNull(), IrConstKind.Double, convertedText as Double, "Incorrect double: $text"
                    )
                }
            CHARACTER_CONSTANT ->
                FirConstExpressionImpl(
                    session, expression.getPsiOrNull(), IrConstKind.Char, text.parseCharacter(), "Incorrect character: $text"
                )
            BOOLEAN_CONSTANT ->
                FirConstExpressionImpl(session, expression.getPsiOrNull(), IrConstKind.Boolean, convertedText as Boolean)
            NULL ->
                FirConstExpressionImpl(session, expression.getPsiOrNull(), IrConstKind.Null, null)
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
                    FirConstExpressionImpl(session, entry.getPsiOrNull(), IrConstKind.String, entry.asText)
                }
                ESCAPE_STRING_TEMPLATE_ENTRY -> {
                    sb.append(entry.unescapedValue)
                    FirConstExpressionImpl(session, entry.getPsiOrNull(), IrConstKind.String, entry.unescapedValue)
                }
                SHORT_STRING_TEMPLATE_ENTRY, LONG_STRING_TEMPLATE_ENTRY -> {
                    hasExpressions = true
                    entry.convertTemplateEntry("Incorrect template argument")
                }
                else -> {
                    hasExpressions = true
                    FirErrorExpressionImpl(
                        session, entry.getPsiOrNull(), "Incorrect template entry: ${entry.asText}"
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
                    FirStringConcatenationCallImpl(session, base).apply {
                        arguments += result!!
                        arguments += nextArgument
                    }
                }
            }
        }
        return if (hasExpressions) result!! else FirConstExpressionImpl(session, base, IrConstKind.String, sb.toString())
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
            return FirErrorExpressionImpl(session, argument, "Inc/dec without operand")
        }
        return FirBlockImpl(session, baseExpression).apply {
            val tempName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(this@BaseFirBuilder.session, baseExpression, tempName, argument.convert())
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = FirFunctionCallImpl(this@BaseFirBuilder.session, baseExpression).apply {
                this.calleeReference = FirSimpleNamedReference(this@BaseFirBuilder.session, baseExpression?.operationReference, callName)
                this.explicitReceiver = generateResolvedAccessExpression(this@BaseFirBuilder.session, baseExpression, temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(this@BaseFirBuilder.session, baseExpression, resultName, resultInitializer)
            val assignment = argument.generateAssignment(
                baseExpression,
                if (prefix && argument.elementType != REFERENCE_EXPRESSION)
                    generateResolvedAccessExpression(this@BaseFirBuilder.session, baseExpression, resultVar)
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
                    statements += generateResolvedAccessExpression(this@BaseFirBuilder.session, baseExpression, resultVar)
                } else {
                    appendAssignment()
                    statements += generateAccessExpression(this@BaseFirBuilder.session, baseExpression, argument.getReferencedNameAsName())
                }
            } else {
                appendAssignment()
                statements += generateResolvedAccessExpression(this@BaseFirBuilder.session, baseExpression, temporaryVariable)
            }
        }
    }

    private fun FirModifiableQualifiedAccess<*>.initializeLValue(
        left: T?,
        convertQualified: T.() -> FirQualifiedAccess?
    ): FirReference {
        val tokenType = left?.elementType
        if (left != null) {
            when (tokenType) {
                REFERENCE_EXPRESSION -> {
                    return FirSimpleNamedReference(this@BaseFirBuilder.session, left.getPsiOrNull(), left.getReferencedNameAsName())
                }
                THIS_EXPRESSION -> {
                    return FirExplicitThisReference(this@BaseFirBuilder.session, left.getPsiOrNull(), left.getLabelName())
                }
                DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> {
                    val firMemberAccess = left.convertQualified()
                    return if (firMemberAccess != null) {
                        explicitReceiver = firMemberAccess.explicitReceiver
                        safe = firMemberAccess.safe
                        firMemberAccess.calleeReference
                    } else {
                        FirErrorNamedReference(
                            this@BaseFirBuilder.session, left.getPsiOrNull(), "Unsupported qualified LValue: ${left.asText}"
                        )
                    }
                }
                PARENTHESIZED -> {
                    return initializeLValue(left.getExpressionInParentheses(), convertQualified)
                }
            }
        }
        return FirErrorNamedReference(this@BaseFirBuilder.session, left.getPsiOrNull(), "Unsupported LValue: $tokenType")
    }

    fun T?.generateAssignment(
        psi: PsiElement?,
        value: FirExpression,
        operation: FirOperation,
        convert: T.() -> FirExpression
    ): FirStatement {
        val tokenType = this?.elementType
        if (tokenType == PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(psi, value, operation, convert)
        }
        if (tokenType == ARRAY_ACCESS_EXPRESSION) {
            val firArrayAccess = this!!.convert() as FirFunctionCallImpl
            val arraySet = if (operation != FirOperation.ASSIGN) {
                FirArraySetCallImpl(session, psi, value, operation).apply {
                    indexes += firArrayAccess.arguments
                }
            } else {
                return firArrayAccess.apply {
                    calleeReference = FirSimpleNamedReference(this@BaseFirBuilder.session, psi, OperatorNameConventions.SET)
                    arguments += value
                }
            }
            val arrayExpression = this.getChildNodeByType(REFERENCE_EXPRESSION)
            if (arrayExpression != null) {
                return arraySet.apply {
                    lValue = initializeLValue(arrayExpression) { convert() as? FirQualifiedAccess }
                }
            }
            return FirBlockImpl(session, arrayExpression).apply {
                val name = Name.special("<array-set>")
                statements += generateTemporaryVariable(
                    this@BaseFirBuilder.session, this@generateAssignment.getPsiOrNull(), name, firArrayAccess.explicitReceiver!!
                )
                statements += arraySet.apply { lValue = FirSimpleNamedReference(this@BaseFirBuilder.session, arrayExpression, name) }
            }
        }
        if (operation != FirOperation.ASSIGN &&
            tokenType != REFERENCE_EXPRESSION && tokenType != THIS_EXPRESSION &&
            ((tokenType != DOT_QUALIFIED_EXPRESSION && tokenType != SAFE_ACCESS_EXPRESSION) || this.selectorExpression?.elementType != REFERENCE_EXPRESSION)
        ) {
            return FirBlockImpl(session, this.getPsiOrNull()).apply {
                val name = Name.special("<complex-set>")
                statements += generateTemporaryVariable(
                    this@BaseFirBuilder.session, this@generateAssignment.getPsiOrNull(), name,
                    this@generateAssignment?.convert()
                        ?: FirErrorExpressionImpl(this@BaseFirBuilder.session, this.getPsiOrNull(), "No LValue in assignment")
                )
                statements += FirVariableAssignmentImpl(this@BaseFirBuilder.session, psi, value, operation).apply {
                    lValue = FirSimpleNamedReference(this@BaseFirBuilder.session, this.getPsiOrNull(), name)
                }
            }
        }
        return FirVariableAssignmentImpl(session, psi, value, operation).apply {
            lValue = initializeLValue(this@generateAssignment) { convert() as? FirQualifiedAccess }
        }
    }
}