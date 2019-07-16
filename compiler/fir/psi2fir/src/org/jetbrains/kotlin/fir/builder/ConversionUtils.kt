/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableAccessorsOwner
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitKPropertyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

fun String.parseCharacter(): Char? {
    // Strip the quotes
    if (length < 2 || this[0] != '\'' || this[length - 1] != '\'') {
        return null
    }
    val text = substring(1, length - 1) // now there're no quotes

    if (text.isEmpty()) {
        return null
    }

    return if (text[0] != '\\') {
        // No escape
        if (text.length == 1) {
            text[0]
        } else {
            null
        }
    } else {
        escapedStringToCharacter(text)
    }
}

internal fun escapedStringToCharacter(text: String): Char? {
    assert(text.isNotEmpty() && text[0] == '\\') {
        "Only escaped sequences must be passed to this routine: $text"
    }

    // Escape
    val escape = text.substring(1) // strip the slash
    when (escape.length) {
        0 -> {
            // bare slash
            return null
        }
        1 -> {
            // one-char escape
            return translateEscape(escape[0]) ?: return null
        }
        5 -> {
            // unicode escape
            if (escape[0] == 'u') {
                try {
                    val intValue = Integer.valueOf(escape.substring(1), 16)
                    return intValue.toInt().toChar()
                } catch (e: NumberFormatException) {
                    // Will be reported below
                }
            }
        }
    }
    return null
}

internal fun translateEscape(c: Char): Char? =
    when (c) {
        't' -> '\t'
        'b' -> '\b'
        'n' -> '\n'
        'r' -> '\r'
        '\'' -> '\''
        '\"' -> '\"'
        '\\' -> '\\'
        '$' -> '$'
        else -> null
    }

internal fun generateConstantExpressionByLiteral(expression: KtConstantExpression): FirExpression {
    val type = expression.node.elementType
    val text: String = expression.text
    val convertedText: Any? = when (type) {
        KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT -> parseNumericLiteral(text, type)
        KtNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
        else -> null
    }
    return when (type) {
        KtNodeTypes.INTEGER_CONSTANT ->
            if (convertedText is Long &&
                (hasLongSuffix(text) || hasUnsignedLongSuffix(text) || hasUnsignedSuffix(text) ||
                        convertedText > Int.MAX_VALUE || convertedText < Int.MIN_VALUE)
            ) {
                FirConstExpressionImpl(
                    expression, IrConstKind.Long, convertedText, "Incorrect long: $text"
                )
            } else if (convertedText is Number) {
                // TODO: support byte / short
                FirConstExpressionImpl(expression, IrConstKind.Int, convertedText.toInt(), "Incorrect int: $text")
            } else {
                FirErrorExpressionImpl(expression, reason = "Incorrect constant expression: $text")
            }
        KtNodeTypes.FLOAT_CONSTANT ->
            if (convertedText is Float) {
                FirConstExpressionImpl(
                    expression, IrConstKind.Float, convertedText, "Incorrect float: $text"
                )
            } else {
                FirConstExpressionImpl(
                    expression, IrConstKind.Double, convertedText as Double, "Incorrect double: $text"
                )
            }
        KtNodeTypes.CHARACTER_CONSTANT ->
            FirConstExpressionImpl(
                expression, IrConstKind.Char, text.parseCharacter(), "Incorrect character: $text"
            )
        KtNodeTypes.BOOLEAN_CONSTANT ->
            FirConstExpressionImpl(expression, IrConstKind.Boolean, convertedText as Boolean)
        KtNodeTypes.NULL ->
            FirConstExpressionImpl(expression, IrConstKind.Null, null)
        else ->
            throw AssertionError("Unknown literal type: $type, $text")
    }

}

fun IElementType.toBinaryName(): Name? {
    return OperatorConventions.BINARY_OPERATION_NAMES[this]
}

fun IElementType.toUnaryName(): Name? {
    return OperatorConventions.UNARY_OPERATION_NAMES[this]
}

fun IElementType.toFirOperation(): FirOperation =
    when (this) {
        KtTokens.LT -> FirOperation.LT
        KtTokens.GT -> FirOperation.GT
        KtTokens.LTEQ -> FirOperation.LT_EQ
        KtTokens.GTEQ -> FirOperation.GT_EQ
        KtTokens.EQEQ -> FirOperation.EQ
        KtTokens.EXCLEQ -> FirOperation.NOT_EQ
        KtTokens.EQEQEQ -> FirOperation.IDENTITY
        KtTokens.EXCLEQEQEQ -> FirOperation.NOT_IDENTITY

        KtTokens.EQ -> FirOperation.ASSIGN
        KtTokens.PLUSEQ -> FirOperation.PLUS_ASSIGN
        KtTokens.MINUSEQ -> FirOperation.MINUS_ASSIGN
        KtTokens.MULTEQ -> FirOperation.TIMES_ASSIGN
        KtTokens.DIVEQ -> FirOperation.DIV_ASSIGN
        KtTokens.PERCEQ -> FirOperation.REM_ASSIGN

        KtTokens.AS_KEYWORD -> FirOperation.AS
        KtTokens.AS_SAFE -> FirOperation.SAFE_AS

        else -> throw AssertionError(this.toString())
    }

fun FirExpression.generateNotNullOrOther(
    session: FirSession, other: FirExpression, caseId: String, basePsi: KtElement?
): FirWhenExpression {
    val subjectName = Name.special("<$caseId>")
    val subjectVariable = generateTemporaryVariable(session, basePsi, subjectName, this)
    val subject = FirWhenSubject()
    val subjectExpression = FirWhenSubjectExpressionImpl(basePsi, subject)
    return FirWhenExpressionImpl(
        basePsi, this, subjectVariable
    ).apply {
        subject.bind(this)
        branches += FirWhenBranchImpl(
            basePsi,
            FirOperatorCallImpl(basePsi, FirOperation.EQ).apply {
                arguments += subjectExpression
                arguments += FirConstExpressionImpl(basePsi, IrConstKind.Null, null)
            },
            FirSingleExpressionBlock(other)
        )
        branches += FirWhenBranchImpl(
            other.psi, FirElseIfTrueCondition(basePsi),
            FirSingleExpressionBlock(
                FirUncheckedNotNullCastImpl(basePsi, generateResolvedAccessExpression(basePsi, subjectVariable))
            )
        )
    }
}

fun FirExpression.generateLazyLogicalOperation(
    other: FirExpression, isAnd: Boolean, basePsi: KtElement?
): FirWhenExpression {
    val terminalExpression = FirConstExpressionImpl(psi, IrConstKind.Boolean, !isAnd)
    val terminalBlock = FirSingleExpressionBlock(terminalExpression)
    val otherBlock = FirSingleExpressionBlock(other)
    return FirWhenExpressionImpl(basePsi).apply {
        branches += FirWhenBranchImpl(
            psi, this@generateLazyLogicalOperation,
            if (isAnd) otherBlock else terminalBlock
        )
        branches += FirWhenBranchImpl(
            other.psi, FirElseIfTrueCondition(psi),
            if (isAnd) terminalBlock else otherBlock
        )
        typeRef = FirImplicitBooleanTypeRef(basePsi)
    }
}

internal fun KtWhenCondition.toFirWhenCondition(
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    val firSubjectExpression = FirWhenSubjectExpressionImpl(this, subject)
    return when (this) {
        is KtWhenConditionWithExpression -> {
            FirOperatorCallImpl(
                expression,
                FirOperation.EQ
            ).apply {
                arguments += firSubjectExpression
                arguments += expression.convert("No expression in condition with expression")
            }
        }
        is KtWhenConditionInRange -> {
            val firRange = rangeExpression.convert("No range in condition with range")
            firRange.generateContainsOperation(firSubjectExpression, isNegated, rangeExpression, operationReference)
        }
        is KtWhenConditionIsPattern -> {
            FirTypeOperatorCallImpl(
                typeReference, if (isNegated) FirOperation.NOT_IS else FirOperation.IS,
                typeReference.toFirOrErrorTypeRef()
            ).apply {
                arguments += firSubjectExpression
            }
        }
        else -> {
            FirErrorExpressionImpl(this, "Unsupported when condition: ${this.javaClass}")
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    basePsi: KtElement,
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    var firCondition: FirExpression? = null
    for (condition in this) {
        val firConditionElement = condition.toFirWhenCondition(subject, convert, toFirOrErrorTypeRef)
        firCondition = when (firCondition) {
            null -> firConditionElement
            else -> firCondition.generateLazyLogicalOperation(
                firConditionElement, false, basePsi
            )
        }
    }
    return firCondition!!
}

internal fun Array<KtStringTemplateEntry>.toInterpolatingCall(
    base: KtStringTemplateExpression,
    convert: KtExpression?.(String) -> FirExpression
): FirExpression {
    val sb = StringBuilder()
    var hasExpressions = false
    var result: FirExpression? = null
    var callCreated = false
    for (entry in this) {
        val nextArgument = when (entry) {
            is KtLiteralStringTemplateEntry -> {
                sb.append(entry.text)
                FirConstExpressionImpl(entry, IrConstKind.String, entry.text)
            }
            is KtEscapeStringTemplateEntry -> {
                sb.append(entry.unescapedValue)
                FirConstExpressionImpl(entry, IrConstKind.String, entry.unescapedValue)
            }
            is KtStringTemplateEntryWithExpression -> {
                val innerExpression = entry.expression
                hasExpressions = true
                innerExpression.convert("Incorrect template argument")
            }
            else -> {
                hasExpressions = true
                FirErrorExpressionImpl(
                    entry, "Incorrect template entry: ${entry.text}"
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
                FirStringConcatenationCallImpl(base).apply {
                    arguments += result!!
                    arguments += nextArgument
                }
            }
        }
    }
    return if (hasExpressions) result!! else FirConstExpressionImpl(base, IrConstKind.String, sb.toString())
}

fun FirExpression.generateContainsOperation(
    argument: FirExpression,
    inverted: Boolean,
    base: KtExpression?,
    operationReference: KtOperationReferenceExpression?
): FirFunctionCall {
    val containsCall = FirFunctionCallImpl(base).apply {
        calleeReference = FirSimpleNamedReference(operationReference, OperatorNameConventions.CONTAINS)
        explicitReceiver = this@generateContainsOperation
        arguments += argument
    }
    if (!inverted) return containsCall
    return FirFunctionCallImpl(base).apply {
        calleeReference = FirSimpleNamedReference(operationReference, OperatorNameConventions.NOT)
        explicitReceiver = containsCall
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
internal fun generateIncrementOrDecrementBlock(
    session: FirSession,
    baseExpression: KtUnaryExpression,
    argument: KtExpression?,
    callName: Name,
    prefix: Boolean,
    convert: KtExpression.() -> FirExpression
): FirExpression {
    if (argument == null) {
        return FirErrorExpressionImpl(argument, "Inc/dec without operand")
    }
    return FirBlockImpl(baseExpression).apply {
        val tempName = Name.special("<unary>")
        val temporaryVariable = generateTemporaryVariable(session, baseExpression, tempName, argument.convert())
        statements += temporaryVariable
        val resultName = Name.special("<unary-result>")
        val resultInitializer = FirFunctionCallImpl(baseExpression).apply {
            this.calleeReference = FirSimpleNamedReference(baseExpression.operationReference, callName)
            this.explicitReceiver = generateResolvedAccessExpression(baseExpression, temporaryVariable)
        }
        val resultVar = generateTemporaryVariable(session, baseExpression, resultName, resultInitializer)
        val assignment = argument.generateAssignment(
            session, baseExpression,
            if (prefix && argument !is KtSimpleNameExpression)
                generateResolvedAccessExpression(baseExpression, resultVar)
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
            if (argument !is KtSimpleNameExpression) {
                statements += resultVar
                appendAssignment()
                statements += generateResolvedAccessExpression(baseExpression, resultVar)
            } else {
                appendAssignment()
                statements += generateAccessExpression(baseExpression, argument.getReferencedNameAsName())
            }
        } else {
            appendAssignment()
            statements += generateResolvedAccessExpression(baseExpression, temporaryVariable)
        }
    }
}

fun generateAccessExpression(psi: PsiElement?, name: Name): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(psi).apply {
        calleeReference = FirSimpleNamedReference(psi, name)
    }

fun generateResolvedAccessExpression(psi: PsiElement?, variable: FirVariable<*>): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(psi).apply {
        calleeReference = FirResolvedCallableReferenceImpl(psi, variable.name, variable.symbol)
    }

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAbstractAnnotatedElement) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    return FirBlockImpl(multiDeclaration).apply {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            statements += FirVariableImpl(
                session, entry, entry.nameAsSafeName,
                entry.typeReference.toFirOrImplicitTypeRef(), isVar,
                FirComponentCallImpl(entry, index + 1, generateResolvedAccessExpression(entry, container)),
                FirVariableSymbol(entry.nameAsSafeName) // TODO?
            ).apply {
                entry.extractAnnotationsTo(this)
                symbol.bind(this)
            }
        }
    }
}

fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, name: Name, initializer: FirExpression
): FirVariable<*> =
    FirVariableImpl(session, psi, name, FirImplicitTypeRefImpl(psi), false, initializer, FirVariableSymbol(name)).apply {
        symbol.bind(this)
    }

internal fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, specialName: String, initializer: FirExpression
): FirVariable<*> = generateTemporaryVariable(session, psi, Name.special("<$specialName>"), initializer)

private fun FirModifiableQualifiedAccess<*>.initializeLValue(
    left: KtExpression?,
    convertQualified: KtQualifiedExpression.() -> FirQualifiedAccess?
): FirReference {
    return when (left) {
        is KtSimpleNameExpression -> {
            FirSimpleNamedReference(left, left.getReferencedNameAsName())
        }
        is KtThisExpression -> {
            FirExplicitThisReference(left, left.getLabelName())
        }
        is KtQualifiedExpression -> {
            val firMemberAccess = left.convertQualified()
            if (firMemberAccess != null) {
                explicitReceiver = firMemberAccess.explicitReceiver
                safe = firMemberAccess.safe
                firMemberAccess.calleeReference
            } else {
                FirErrorNamedReference(left, "Unsupported qualified LValue: ${left.text}")
            }
        }
        is KtParenthesizedExpression -> {
            initializeLValue(left.expression, convertQualified)
        }
        else -> {
            FirErrorNamedReference(left, "Unsupported LValue: ${left?.javaClass}")
        }
    }
}

internal fun KtExpression?.generateAssignment(
    session: FirSession,
    psi: PsiElement?,
    value: FirExpression,
    operation: FirOperation,
    convert: KtExpression.() -> FirExpression
): FirStatement {
    if (this is KtParenthesizedExpression) {
        return expression.generateAssignment(session, psi, value, operation, convert)
    }
    if (this is KtArrayAccessExpression) {
        val arrayExpression = this.arrayExpression
        val firArrayExpression = arrayExpression?.convert() ?: FirErrorExpressionImpl(arrayExpression, "No array expression")
        val arraySet = if (operation != FirOperation.ASSIGN) {
            FirArraySetCallImpl(psi, value, operation).apply {
                for (indexExpression in indexExpressions) {
                    indexes += indexExpression.convert()
                }
            }
        } else {
            return FirFunctionCallImpl(psi).apply {
                calleeReference = FirSimpleNamedReference(psi, OperatorNameConventions.SET)
                explicitReceiver = firArrayExpression
                for (indexExpression in indexExpressions) {
                    arguments += indexExpression.convert()
                }
                arguments += value
            }
        }
        if (arrayExpression is KtSimpleNameExpression) {
            return arraySet.apply {
                lValue = initializeLValue(arrayExpression) { convert() as? FirQualifiedAccess }
            }
        }
        return FirBlockImpl(arrayExpression).apply {
            val name = Name.special("<array-set>")
            statements += generateTemporaryVariable(session, this@generateAssignment, name, firArrayExpression)
            statements += arraySet.apply { lValue = FirSimpleNamedReference(arrayExpression, name) }
        }
    }
    if (operation != FirOperation.ASSIGN &&
        this !is KtSimpleNameExpression && this !is KtThisExpression &&
        (this !is KtQualifiedExpression || selectorExpression !is KtSimpleNameExpression)
    ) {
        return FirBlockImpl(this).apply {
            val name = Name.special("<complex-set>")
            statements += generateTemporaryVariable(
                session, this@generateAssignment, name,
                this@generateAssignment?.convert() ?: FirErrorExpressionImpl(this@generateAssignment, "No LValue in assignment")
            )
            statements += FirVariableAssignmentImpl(psi, value, operation).apply {
                lValue = FirSimpleNamedReference(this@generateAssignment, name)
            }
        }
    }
    return FirVariableAssignmentImpl(psi, value, operation).apply {
        lValue = initializeLValue(this@generateAssignment) { convert() as? FirQualifiedAccess }
    }
}

internal fun FirModifiableAccessorsOwner.generateAccessorsByDelegate(session: FirSession, member: Boolean, stubMode: Boolean) {
    val variable = this as FirVariable<*>
    val delegateFieldSymbol = delegateFieldSymbol ?: return
    val delegate = delegate as? FirWrappedDelegateExpressionImpl ?: return
    fun delegateAccess() = FirQualifiedAccessExpressionImpl(null).apply {
        calleeReference = FirDelegateFieldReferenceImpl(null, delegateFieldSymbol)
    }

    fun thisRef() =
        if (member) FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference = FirExplicitThisReference(null, null)
        }
        else FirConstExpressionImpl(null, IrConstKind.Null, null)

    fun propertyRef() = FirCallableReferenceAccessImpl(null).apply {
        calleeReference = FirResolvedCallableReferenceImpl(null, variable.name, variable.symbol)
        typeRef = FirImplicitKPropertyTypeRef(null, ConeStarProjection)
    }

    delegate.delegateProvider = if (stubMode) FirExpressionStub(null) else FirFunctionCallImpl(null).apply {
        explicitReceiver = delegate.expression
        calleeReference = FirSimpleNamedReference(null, PROVIDE_DELEGATE)
        arguments += thisRef()
        arguments += propertyRef()
    }
    if (stubMode) return
    getter = (getter as? FirPropertyAccessorImpl)
        ?: FirPropertyAccessorImpl(session, null, true, Visibilities.UNKNOWN, FirImplicitTypeRefImpl(null)).apply Accessor@{
            body = FirSingleExpressionBlock(
                FirReturnExpressionImpl(
                    null,
                    FirFunctionCallImpl(null).apply {
                        explicitReceiver = delegateAccess()
                        calleeReference = FirSimpleNamedReference(null, GET_VALUE)
                        arguments += thisRef()
                        arguments += propertyRef()
                    }
                ).apply {
                    target = FirFunctionTarget(null)
                    target.bind(this@Accessor)
                }
            )
        }
    setter = (setter as? FirPropertyAccessorImpl)
        ?: FirPropertyAccessorImpl(session, null, false, Visibilities.UNKNOWN, FirImplicitUnitTypeRef(null)).apply {
            val parameter = FirValueParameterImpl(
                session, null, DELEGATED_SETTER_PARAM,
                FirImplicitTypeRefImpl(null),
                defaultValue = null, isCrossinline = false,
                isNoinline = false, isVararg = false
            )
            valueParameters += parameter
            body = FirSingleExpressionBlock(
                FirFunctionCallImpl(null).apply {
                    explicitReceiver = delegateAccess()
                    calleeReference = FirSimpleNamedReference(null, SET_VALUE)
                    arguments += thisRef()
                    arguments += propertyRef()
                    arguments += FirQualifiedAccessExpressionImpl(null).apply {
                        calleeReference = FirResolvedCallableReferenceImpl(psi, DELEGATED_SETTER_PARAM, parameter.symbol)
                    }
                }
            )
        }
}

private val GET_VALUE = Name.identifier("getValue")
private val SET_VALUE = Name.identifier("setValue")
private val PROVIDE_DELEGATE = Name.identifier("provideDelegate")
private val DELEGATED_SETTER_PARAM = Name.special("<set-?>")