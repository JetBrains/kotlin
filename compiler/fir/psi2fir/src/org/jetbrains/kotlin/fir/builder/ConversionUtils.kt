/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions

internal fun String.parseCharacter(): Char? {
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

internal fun generateConstantExpressionByLiteral(session: FirSession, expression: KtConstantExpression): FirExpression {
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
                    session, expression, IrConstKind.Long, convertedText, "Incorrect long: $text"
                )
            } else if (convertedText is Number) {
                // TODO: support byte / short
                FirConstExpressionImpl(session, expression, IrConstKind.Int, convertedText.toInt(), "Incorrect int: $text")
            } else {
                FirErrorExpressionImpl(session, expression, reason = "Incorrect constant expression: $text")
            }
        KtNodeTypes.FLOAT_CONSTANT ->
            if (convertedText is Float) {
                FirConstExpressionImpl(
                    session, expression, IrConstKind.Float, convertedText, "Incorrect float: $text"
                )
            } else {
                FirConstExpressionImpl(
                    session, expression, IrConstKind.Double, convertedText as Double, "Incorrect double: $text"
                )
            }
        KtNodeTypes.CHARACTER_CONSTANT ->
            FirConstExpressionImpl(
                session, expression, IrConstKind.Char, text.parseCharacter(), "Incorrect character: $text"
            )
        KtNodeTypes.BOOLEAN_CONSTANT ->
            FirConstExpressionImpl(session, expression, IrConstKind.Boolean, convertedText as Boolean)
        KtNodeTypes.NULL ->
            FirConstExpressionImpl(session, expression, IrConstKind.Null, null)
        else ->
            throw AssertionError("Unknown literal type: $type, $text")
    }

}

internal fun IElementType.toBinaryName(): Name? {
    return OperatorConventions.BINARY_OPERATION_NAMES[this]
}

internal fun IElementType.toUnaryName(): Name? {
    return OperatorConventions.UNARY_OPERATION_NAMES[this]
}

internal fun IElementType.toFirOperation(): FirOperation =
    when (this) {
        KtTokens.LT -> FirOperation.LT
        KtTokens.GT -> FirOperation.GT
        KtTokens.LTEQ -> FirOperation.LT_EQ
        KtTokens.GTEQ -> FirOperation.GT_EQ
        KtTokens.EQEQ -> FirOperation.EQ
        KtTokens.EXCLEQ -> FirOperation.NOT_EQ
        KtTokens.EQEQEQ -> FirOperation.IDENTITY
        KtTokens.EXCLEQEQEQ -> FirOperation.NOT_IDENTITY
        KtTokens.IN_KEYWORD -> FirOperation.IN
        KtTokens.NOT_IN -> FirOperation.NOT_IN
        KtTokens.RANGE -> FirOperation.RANGE

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

internal fun FirExpression.generateNotNullOrOther(
    session: FirSession, other: FirExpression, caseId: String, basePsi: KtElement
): FirWhenExpression {
    val subjectName = Name.special("<$caseId>")
    val subjectVariable = generateTemporaryVariable(session, psi, subjectName, this)
    val subjectExpression = FirWhenSubjectExpression(session, psi)
    return FirWhenExpressionImpl(
        session, basePsi, this, subjectVariable
    ).apply {
        branches += FirWhenBranchImpl(
            session, psi,
            FirOperatorCallImpl(session, psi, FirOperation.EQ).apply {
                arguments += subjectExpression
                arguments += FirConstExpressionImpl(session, psi, IrConstKind.Null, null)
            },
            FirSingleExpressionBlock(session, other)
        )
        branches += FirWhenBranchImpl(
            session, other.psi, FirElseIfTrueCondition(session, psi),
            FirSingleExpressionBlock(session, generateAccessExpression(session, psi, subjectName))
        )
    }
}

internal fun FirExpression.generateLazyLogicalOperation(
    session: FirSession, other: FirExpression, isAnd: Boolean, basePsi: KtElement
): FirWhenExpression {
    val terminalExpression = FirConstExpressionImpl(session, psi, IrConstKind.Boolean, !isAnd)
    val terminalBlock = FirSingleExpressionBlock(session, terminalExpression)
    val otherBlock = FirSingleExpressionBlock(session, other)
    return FirWhenExpressionImpl(session, basePsi).apply {
        branches += FirWhenBranchImpl(
            session, psi, this@generateLazyLogicalOperation,
            if (isAnd) otherBlock else terminalBlock
        )
        branches += FirWhenBranchImpl(
            session, other.psi, FirElseIfTrueCondition(session, psi),
            if (isAnd) terminalBlock else otherBlock
        )
        typeRef = FirImplicitBooleanTypeRef(session, basePsi)
    }
}

internal fun KtWhenCondition.toFirWhenCondition(
    session: FirSession,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    val firSubjectExpression = FirWhenSubjectExpression(session, this)
    return when (this) {
        is KtWhenConditionWithExpression -> {
            FirOperatorCallImpl(
                session,
                expression,
                FirOperation.EQ
            ).apply {
                arguments += firSubjectExpression
                arguments += expression.convert("No expression in condition with expression")
            }
        }
        is KtWhenConditionInRange -> {
            FirOperatorCallImpl(
                session,
                rangeExpression,
                if (isNegated) FirOperation.NOT_IN else FirOperation.IN
            ).apply {
                arguments += firSubjectExpression
                arguments += rangeExpression.convert("No range in condition with range")
            }
        }
        is KtWhenConditionIsPattern -> {
            FirTypeOperatorCallImpl(
                session, typeReference, if (isNegated) FirOperation.NOT_IS else FirOperation.IS,
                typeReference.toFirOrErrorTypeRef()
            ).apply {
                arguments += firSubjectExpression
            }
        }
        else -> {
            FirErrorExpressionImpl(session, this, "Unsupported when condition: ${this.javaClass}")
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    session: FirSession,
    basePsi: KtElement,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    var firCondition: FirExpression? = null
    for (condition in this) {
        val firConditionElement = condition.toFirWhenCondition(session, convert, toFirOrErrorTypeRef)
        firCondition = when (firCondition) {
            null -> firConditionElement
            else -> firCondition.generateLazyLogicalOperation(
                session, firConditionElement, false, basePsi
            )
        }
    }
    return firCondition!!
}

internal fun generateIncrementOrDecrementBlock(
    session: FirSession,
    baseExpression: KtUnaryExpression,
    argument: KtExpression?,
    callName: Name,
    prefix: Boolean,
    convert: KtExpression.() -> FirExpression
): FirExpression {
    if (argument == null) {
        return FirErrorExpressionImpl(session, argument, "Inc/dec without operand")
    }
    return FirBlockImpl(session, baseExpression).apply {
        val tempName = Name.special("<unary>")
        statements += generateTemporaryVariable(session, baseExpression, tempName, argument.convert())
        val resultName = Name.special("<unary-result>")
        val resultInitializer = FirFunctionCallImpl(session, baseExpression).apply {
            this.calleeReference = FirSimpleNamedReference(session, baseExpression.operationReference, callName)
            this.arguments += generateAccessExpression(session, baseExpression, tempName)
        }
        val resultVar = generateTemporaryVariable(session, baseExpression, resultName, resultInitializer)
        val assignment = argument.generateAssignment(
            session, baseExpression,
            if (prefix && argument !is KtSimpleNameExpression)
                generateAccessExpression(session, baseExpression, resultName)
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
                statements += generateAccessExpression(session, baseExpression, resultName)
            } else {
                appendAssignment()
                statements += generateAccessExpression(session, baseExpression, argument.getReferencedNameAsName())
            }
        } else {
            appendAssignment()
            statements += generateAccessExpression(session, baseExpression, tempName)
        }
    }
}

internal fun generateAccessExpression(session: FirSession, psi: PsiElement?, name: Name): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(session, psi).apply {
        calleeReference = FirSimpleNamedReference(session, psi, name)
    }

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAbstractAnnotatedElement) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    return FirBlockImpl(session, multiDeclaration).apply {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            statements += FirVariableImpl(
                session, entry, entry.nameAsSafeName,
                entry.typeReference.toFirOrImplicitTypeRef(), isVar,
                FirComponentCallImpl(session, entry, index + 1, generateAccessExpression(session, entry, container.name)),
                FirVariableSymbol(entry.nameAsSafeName) // TODO?
            ).apply {
                entry.extractAnnotationsTo(this)
                symbol.bind(this)
            }
        }
    }
}

internal fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, name: Name, initializer: FirExpression
): FirVariable =
    FirVariableImpl(session, psi, name, FirImplicitTypeRefImpl(session, psi), false, initializer, FirVariableSymbol(name)).apply {
        symbol.bind(this)
    }

internal fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, specialName: String, initializer: FirExpression
): FirVariable = generateTemporaryVariable(session, psi, Name.special("<$specialName>"), initializer)

private fun FirModifiableQualifiedAccess.initializeLValue(
    session: FirSession,
    left: KtExpression?,
    convertQualified: KtQualifiedExpression.() -> FirQualifiedAccess?
): FirReference {
    return when (left) {
        is KtSimpleNameExpression -> {
            FirSimpleNamedReference(session, left, left.getReferencedNameAsName())
        }
        is KtThisExpression -> {
            FirExplicitThisReference(session, left, left.getLabelName())
        }
        is KtQualifiedExpression -> {
            val firMemberAccess = left.convertQualified()
            if (firMemberAccess != null) {
                explicitReceiver = firMemberAccess.explicitReceiver
                safe = firMemberAccess.safe
                firMemberAccess.calleeReference
            } else {
                FirErrorNamedReference(session, left, "Unsupported qualified LValue: ${left.text}")
            }
        }
        is KtParenthesizedExpression -> {
            initializeLValue(session, left.expression, convertQualified)
        }
        else -> {
            FirErrorNamedReference(session, left, "Unsupported LValue: ${left?.javaClass}")
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
        val arraySet = FirArraySetCallImpl(session, psi, value, operation).apply {
            for (indexExpression in indexExpressions) {
                indexes += indexExpression.convert()
            }
        }
        if (arrayExpression is KtSimpleNameExpression) {
            return arraySet.apply {
                lValue = initializeLValue(session, arrayExpression) { convert() as? FirQualifiedAccess }
            }
        }
        return FirBlockImpl(session, arrayExpression).apply {
            val name = Name.special("<array-set>")
            statements += generateTemporaryVariable(
                session, this@generateAssignment, name,
                arrayExpression?.convert() ?: FirErrorExpressionImpl(session, arrayExpression, "No array expression")
            )
            statements += arraySet.apply { lValue = FirSimpleNamedReference(session, arrayExpression, name) }
        }
    }
    if (operation != FirOperation.ASSIGN &&
        this !is KtSimpleNameExpression && this !is KtThisExpression &&
        (this !is KtQualifiedExpression || selectorExpression !is KtSimpleNameExpression)
    ) {
        return FirBlockImpl(session, this).apply {
            val name = Name.special("<complex-set>")
            statements += generateTemporaryVariable(
                session, this@generateAssignment, name,
                this@generateAssignment?.convert() ?: FirErrorExpressionImpl(session, this@generateAssignment, "No LValue in assignment")
            )
            statements += FirVariableAssignmentImpl(session, psi, value, operation).apply {
                lValue = FirSimpleNamedReference(session, this@generateAssignment, name)
            }
        }
    }
    return FirVariableAssignmentImpl(session, psi, value, operation).apply {
        lValue = initializeLValue(session, this@generateAssignment) { convert() as? FirQualifiedAccess }
    }
}