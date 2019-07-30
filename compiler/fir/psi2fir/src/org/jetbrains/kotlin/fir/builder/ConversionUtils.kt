/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
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

fun escapedStringToCharacter(text: String): Char? {
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
    val subjectVariable = generateTemporaryVariable(session, psi, subjectName, this)
    val subject = FirWhenSubject()
    val subjectExpression = FirWhenSubjectExpressionImpl(session, psi, subject)
    return FirWhenExpressionImpl(
        session, basePsi, this, subjectVariable
    ).apply {
        subject.bind(this)
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
            FirSingleExpressionBlock(
                session,
                FirUncheckedNotNullCastImpl(session, psi, generateResolvedAccessExpression(session, psi, subjectVariable))
            )
        )
    }
}

fun FirExpression.generateLazyLogicalOperation(
    session: FirSession, other: FirExpression, isAnd: Boolean, basePsi: KtElement?
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
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    val firSubjectExpression = FirWhenSubjectExpressionImpl(session, this, subject)
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
            val firRange = rangeExpression.convert("No range in condition with range")
            firRange.generateContainsOperation(session, firSubjectExpression, isNegated, rangeExpression, operationReference)
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
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    var firCondition: FirExpression? = null
    for (condition in this) {
        val firConditionElement = condition.toFirWhenCondition(session, subject, convert, toFirOrErrorTypeRef)
        firCondition = when (firCondition) {
            null -> firConditionElement
            else -> firCondition.generateLazyLogicalOperation(
                session, firConditionElement, false, basePsi
            )
        }
    }
    return firCondition!!
}

fun FirExpression.generateContainsOperation(
    session: FirSession,
    argument: FirExpression,
    inverted: Boolean,
    base: KtExpression?,
    operationReference: KtOperationReferenceExpression?
): FirFunctionCall {
    val containsCall = FirFunctionCallImpl(session, base).apply {
        calleeReference = FirSimpleNamedReference(session, operationReference, OperatorNameConventions.CONTAINS)
        explicitReceiver = this@generateContainsOperation
        arguments += argument
    }
    if (!inverted) return containsCall
    return FirFunctionCallImpl(session, base).apply {
        calleeReference = FirSimpleNamedReference(session, operationReference, OperatorNameConventions.NOT)
        explicitReceiver = containsCall
    }
}

fun generateAccessExpression(session: FirSession, psi: PsiElement?, name: Name): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(session, psi).apply {
        calleeReference = FirSimpleNamedReference(session, psi, name)
    }

fun generateResolvedAccessExpression(session: FirSession, psi: PsiElement?, variable: FirVariable<*>): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(session, psi).apply {
        calleeReference = FirResolvedCallableReferenceImpl(session, psi, variable.name, variable.symbol)
    }

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable<*>,
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
                FirComponentCallImpl(session, entry, index + 1, generateResolvedAccessExpression(session, entry, container)),
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
    FirVariableImpl(session, psi, name, FirImplicitTypeRefImpl(session, psi), false, initializer, FirVariableSymbol(name)).apply {
        symbol.bind(this)
    }

fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, specialName: String, initializer: FirExpression
): FirVariable<*> = generateTemporaryVariable(session, psi, Name.special("<$specialName>"), initializer)
