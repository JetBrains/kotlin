/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.impl.FirDelegateFieldReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitKPropertyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
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
    session: FirSession, other: FirExpression, caseId: String, baseSource: FirSourceElement?
): FirWhenExpression {
    val subjectName = Name.special("<$caseId>")
    val subjectVariable = generateTemporaryVariable(session, baseSource, subjectName, this)
    val subject = FirWhenSubject()
    val subjectExpression = FirWhenSubjectExpressionImpl(baseSource, subject)
    return FirWhenExpressionImpl(
        baseSource, this, subjectVariable
    ).apply {
        subject.bind(this)
        branches += FirWhenBranchImpl(
            baseSource,
            FirOperatorCallImpl(baseSource, FirOperation.EQ).apply {
                arguments += subjectExpression
                arguments += FirConstExpressionImpl(baseSource, FirConstKind.Null, null)
            },
            FirSingleExpressionBlock(other)
        )
        branches += FirWhenBranchImpl(
            other.source, FirElseIfTrueCondition(baseSource),
            FirSingleExpressionBlock(
                generateResolvedAccessExpression(baseSource, subjectVariable)
            )
        )
    }
}

fun FirExpression.generateLazyLogicalOperation(
    other: FirExpression, isAnd: Boolean, baseSource: FirSourceElement?
): FirBinaryLogicExpression {
    val kind = if (isAnd)
        LogicOperationKind.AND
    else
        LogicOperationKind.OR
    return FirBinaryLogicExpressionImpl(baseSource, this, other, kind)
}

internal fun KtWhenCondition.toFirWhenCondition(
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    val firSubjectExpression = FirWhenSubjectExpressionImpl(this.toFirSourceElement(), subject)
    return when (this) {
        is KtWhenConditionWithExpression -> {
            FirOperatorCallImpl(
                expression?.toFirSourceElement(),
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
                typeReference?.toFirSourceElement(),
                if (isNegated) FirOperation.NOT_IS else FirOperation.IS,
                typeReference.toFirOrErrorTypeRef()
            ).apply {
                arguments += firSubjectExpression
            }
        }
        else -> {
            FirErrorExpressionImpl(this.toFirSourceElement(), FirSimpleDiagnostic("Unsupported when condition: ${this.javaClass}", DiagnosticKind.Syntax))
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    baseSource: FirSourceElement?,
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
                firConditionElement, false, baseSource
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
    val baseSource = base?.toFirSourceElement()
    val operationReferenceSource = operationReference?.toFirSourceElement()
    val containsCall = FirFunctionCallImpl(baseSource).apply {
        calleeReference = FirSimpleNamedReference(operationReferenceSource, OperatorNameConventions.CONTAINS, null)
        explicitReceiver = this@generateContainsOperation
        arguments += argument
    }
    if (!inverted) return containsCall
    return FirFunctionCallImpl(baseSource).apply {
        calleeReference = FirSimpleNamedReference(operationReferenceSource, OperatorNameConventions.NOT, null)
        explicitReceiver = containsCall
    }
}

fun generateAccessExpression(source: FirSourceElement?, name: Name): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(source).apply {
        calleeReference = FirSimpleNamedReference(source, name, null)
    }

fun generateResolvedAccessExpression(source: FirSourceElement?, variable: FirVariable<*>): FirQualifiedAccessExpression =
    FirQualifiedAccessExpressionImpl(source).apply {
        calleeReference = FirResolvedNamedReferenceImpl(source, variable.name, variable.symbol)
    }

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAbstractAnnotatedElement) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef
): FirExpression {
    return FirBlockImpl(multiDeclaration.toFirSourceElement()).apply {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            val entrySource = entry.toFirSourceElement()
            statements += FirPropertyImpl(
                entrySource,
                session,
                entry.typeReference.toFirOrImplicitTypeRef(),
                null,
                entry.nameAsSafeName,
                FirComponentCallImpl(entrySource, generateResolvedAccessExpression(entrySource, container), index + 1),
                null,
                isVar,
                FirPropertySymbol(entry.nameAsSafeName), // TODO?
                true,
                FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
            ).apply {
                entry.extractAnnotationsTo(this)
                symbol.bind(this)
            }
        }
    }
}

fun generateTemporaryVariable(
    session: FirSession, source: FirSourceElement?, name: Name, initializer: FirExpression
): FirVariable<*> =
    FirPropertyImpl(
        source,
        session,
        FirImplicitTypeRefImpl(source),
        null,
        name,
        initializer,
        null,
        false,
        FirPropertySymbol(name),
        true,
        FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
    ).apply {
        symbol.bind(this)
    }

fun generateTemporaryVariable(
    session: FirSession, source: FirSourceElement?, specialName: String, initializer: FirExpression
): FirVariable<*> = generateTemporaryVariable(session, source, Name.special("<$specialName>"), initializer)

fun FirModifiableVariable<*>.generateAccessorsByDelegate(session: FirSession, member: Boolean, stubMode: Boolean, receiver: FirExpression?) {
    val variable = this as FirVariable<*>
    val delegateFieldSymbol = delegateFieldSymbol ?: return
    val delegate = delegate as? FirWrappedDelegateExpressionImpl ?: return
    fun delegateAccess() = FirQualifiedAccessExpressionImpl(null).apply {
        calleeReference = FirDelegateFieldReferenceImpl(null, null, delegateFieldSymbol)
    }

    fun thisRef(): FirExpression =
        if (member) FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference = FirExplicitThisReference(null, null)
        }
        else FirConstExpressionImpl(null, FirConstKind.Null, null)

    fun propertyRef() = FirCallableReferenceAccessImpl(null).apply {
        calleeReference = FirResolvedNamedReferenceImpl(null, variable.name, variable.symbol)
        typeRef = FirImplicitKPropertyTypeRef(null, ConeStarProjection)
    }

    delegate.delegateProvider = if (stubMode) FirExpressionStub(null) else FirFunctionCallImpl(null).apply {
        explicitReceiver = receiver
        calleeReference = FirSimpleNamedReference(null, PROVIDE_DELEGATE, null)
        arguments += thisRef()
        arguments += propertyRef()
    }
    if (stubMode) return
    if (getter == null || getter is FirDefaultPropertyAccessor) {
        getter = FirPropertyAccessorImpl(
            null,
            session,
            FirImplicitTypeRefImpl(null),
            FirPropertyAccessorSymbol(),
            true,
            FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)
        ).apply Accessor@{
            body = FirSingleExpressionBlock(
                FirReturnExpressionImpl(
                    null,
                    FirFunctionCallImpl(null).apply {
                        explicitReceiver = delegateAccess()
                        calleeReference = FirSimpleNamedReference(null, GET_VALUE, null)
                        arguments += thisRef()
                        arguments += propertyRef()
                    }
                ).apply {
                    target = FirFunctionTarget(null)
                    target.bind(this@Accessor)
                }
            )
        }
    }
    if (variable.isVar && (setter == null || setter is FirDefaultPropertyAccessor)) {
        setter = FirPropertyAccessorImpl(
            null,
            session,
            session.builtinTypes.unitType,
            FirPropertyAccessorSymbol(),
            false,
            FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)
        ).apply {
            val parameter = FirValueParameterImpl(
                null, session, FirImplicitTypeRefImpl(null),
                DELEGATED_SETTER_PARAM, FirVariableSymbol(name),
                defaultValue = null, isCrossinline = false,
                isNoinline = false, isVararg = false
            )
            valueParameters += parameter
            body = FirSingleExpressionBlock(
                FirFunctionCallImpl(null).apply {
                    explicitReceiver = delegateAccess()
                    calleeReference = FirSimpleNamedReference(null, SET_VALUE, null)
                    arguments += thisRef()
                    arguments += propertyRef()
                    arguments += FirQualifiedAccessExpressionImpl(null).apply {
                        calleeReference = FirResolvedNamedReferenceImpl(source, DELEGATED_SETTER_PARAM, parameter.symbol)
                    }
                }
            )
        }
    }
}

private val GET_VALUE = Name.identifier("getValue")
private val SET_VALUE = Name.identifier("setValue")
private val PROVIDE_DELEGATE = Name.identifier("provideDelegate")
private val DELEGATED_SETTER_PARAM = Name.special("<set-?>")
