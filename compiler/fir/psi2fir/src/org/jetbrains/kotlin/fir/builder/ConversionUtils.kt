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

fun generateTemporaryVariable(
    session: FirSession, psi: PsiElement?, specialName: String, initializer: FirExpression
): FirVariable<*> = generateTemporaryVariable(session, psi, Name.special("<$specialName>"), initializer)
