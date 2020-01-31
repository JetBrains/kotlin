/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.FirPropertyBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.builder.buildDelegateFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitKPropertyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.Variance
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
    session: FirSession, other: FirExpression, caseId: String, baseSource: FirSourceElement?,
): FirWhenExpression {
    val subjectName = Name.special("<$caseId>")
    val subjectVariable = generateTemporaryVariable(session, baseSource, subjectName, this)
    val subject = FirWhenSubject()
    val subjectExpression = buildWhenSubjectExpression {
        source = baseSource
        whenSubject = subject
    }

    return buildWhenExpression {
        source = baseSource
        this.subject = this@generateNotNullOrOther
        this.subjectVariable = subjectVariable
        branches += buildWhenBranch {
            source = baseSource
            condition = buildOperatorCall {
                source = baseSource
                operation = FirOperation.EQ
                arguments += subjectExpression
                arguments += buildConstExpression(baseSource, FirConstKind.Null, null)
            }
            result = buildSingleExpressionBlock(other)
        }
        branches += buildWhenBranch {
            source = other.source
            condition = buildElseIfTrueCondition {
                source = baseSource
            }
            result = buildSingleExpressionBlock(generateResolvedAccessExpression(baseSource, subjectVariable))
        }
    }.also {
        subject.bind(it)
    }
}

fun FirExpression.generateLazyLogicalOperation(
    other: FirExpression, isAnd: Boolean, baseSource: FirSourceElement?,
): FirBinaryLogicExpression {
    return buildBinaryLogicExpression {
        source = baseSource
        leftOperand = this@generateLazyLogicalOperation
        rightOperand = other
        kind = if (isAnd) LogicOperationKind.AND else LogicOperationKind.OR
    }
}

internal fun KtWhenCondition.toFirWhenCondition(
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    val baseSource = this.toFirSourceElement()
    val firSubjectExpression = buildWhenSubjectExpression {
        source = baseSource
        whenSubject = subject
    }
    return when (this) {
        is KtWhenConditionWithExpression -> {
            buildOperatorCall {
                source = expression?.toFirSourceElement()
                operation = FirOperation.EQ
                arguments += firSubjectExpression
                arguments += expression.convert("No expression in condition with expression")
            }
        }
        is KtWhenConditionInRange -> {
            val firRange = rangeExpression.convert("No range in condition with range")
            firRange.generateContainsOperation(firSubjectExpression, isNegated, rangeExpression, operationReference)
        }
        is KtWhenConditionIsPattern -> {
            buildTypeOperatorCall {
                source = typeReference?.toFirSourceElement()
                operation = if (isNegated) FirOperation.NOT_IS else FirOperation.IS
                conversionTypeRef = typeReference.toFirOrErrorTypeRef()
                arguments += firSubjectExpression
            }
        }
        else -> {
            buildErrorExpression(baseSource, FirSimpleDiagnostic("Unsupported when condition: ${this.javaClass}", DiagnosticKind.Syntax))
        }
    }
}

internal fun Array<KtWhenCondition>.toFirWhenCondition(
    baseSource: FirSourceElement?,
    subject: FirWhenSubject,
    convert: KtExpression?.(String) -> FirExpression,
    toFirOrErrorTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    var firCondition: FirExpression? = null
    for (condition in this) {
        val firConditionElement = condition.toFirWhenCondition(subject, convert, toFirOrErrorTypeRef)
        firCondition = when (firCondition) {
            null -> firConditionElement
            else -> firCondition.generateLazyLogicalOperation(
                firConditionElement, false, baseSource,
            )
        }
    }
    return firCondition!!
}

fun FirExpression.generateContainsOperation(
    argument: FirExpression,
    inverted: Boolean,
    base: KtExpression?,
    operationReference: KtOperationReferenceExpression?,
): FirFunctionCall {
    val baseSource = base?.toFirSourceElement()
    val operationReferenceSource = operationReference?.toFirSourceElement()
    val containsCall = buildFunctionCall {
        source = baseSource
        calleeReference = buildSimpleNamedReference {
            source = operationReferenceSource
            name = OperatorNameConventions.CONTAINS
        }
        explicitReceiver = this@generateContainsOperation
        arguments += argument
    }
    if (!inverted) return containsCall
    return buildFunctionCall {
        source = baseSource
        calleeReference = buildSimpleNamedReference {
            source = operationReferenceSource
            name = OperatorNameConventions.NOT
        }
        explicitReceiver = containsCall
    }
}

fun generateAccessExpression(source: FirSourceElement?, name: Name): FirQualifiedAccessExpression =
    buildQualifiedAccessExpression {
        this.source = source
        calleeReference = buildSimpleNamedReference {
            this.source = source
            this.name = name
        }
    }

fun generateResolvedAccessExpression(source: FirSourceElement?, variable: FirVariable<*>): FirQualifiedAccessExpression =
    buildQualifiedAccessExpression {
        this.source = source
        calleeReference = buildResolvedNamedReference {
            this.source = source
            name = variable.name
            resolvedSymbol = variable.symbol
        }
    }

internal fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: KtDestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean,
    extractAnnotationsTo: KtAnnotated.(FirAnnotationContainerBuilder) -> Unit,
    toFirOrImplicitTypeRef: KtTypeReference?.() -> FirTypeRef,
): FirExpression {
    return buildBlock {
        source = multiDeclaration.toFirSourceElement()
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            val entrySource = entry.toFirSourceElement()
            val name = entry.nameAsSafeName
            statements += buildProperty {
                source = entrySource
                this.session = session
                returnTypeRef = entry.typeReference.toFirOrImplicitTypeRef()
                this.name = name
                initializer = buildComponentCall {
                    source = entrySource
                    explicitReceiver = generateResolvedAccessExpression(entrySource, container)
                    componentIndex = index + 1
                }
                this.isVar = isVar
                isLocal = true
                status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
                symbol = FirPropertySymbol(name)
                entry.extractAnnotationsTo(this)
            }
        }
    }
}

fun generateTemporaryVariable(
    session: FirSession, source: FirSourceElement?, name: Name, initializer: FirExpression, typeRef: FirTypeRef? = null,
): FirVariable<*> =
    buildProperty {
        this.source = source
        this.session = session
        returnTypeRef = typeRef ?: buildImplicitTypeRef {
            this.source = source
        }
        this.name = name
        this.initializer = initializer
        symbol = FirPropertySymbol(name)
        isVar = false
        isLocal = true
        status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
    }

fun generateTemporaryVariable(
    session: FirSession, source: FirSourceElement?, specialName: String, initializer: FirExpression,
): FirVariable<*> = generateTemporaryVariable(session, source, Name.special("<$specialName>"), initializer)

fun FirPropertyBuilder.generateAccessorsByDelegate(
    delegateBuilder: FirWrappedDelegateExpressionBuilder?,
    session: FirSession,
    member: Boolean,
    extension: Boolean,
    stubMode: Boolean,
    receiver: FirExpression?

) {
    if (delegateBuilder == null) return
    val delegateFieldSymbol = FirDelegateFieldSymbol<FirProperty>(symbol.callableId).also {
        this.delegateFieldSymbol = it
    }
    fun delegateAccess() = buildQualifiedAccessExpression {
        source = null
        calleeReference = buildDelegateFieldReference {
            resolvedSymbol = delegateFieldSymbol
        }
    }

    fun thisRef(): FirExpression =
        if (member || extension) buildQualifiedAccessExpression {
            source = null
            calleeReference = buildExplicitThisReference {}
        }
        else buildConstExpression(null, FirConstKind.Null, null)

    fun propertyRef() = buildCallableReferenceAccess {
        source = null
        calleeReference = buildResolvedNamedReference {
            source = null
            name = this@generateAccessorsByDelegate.name
            resolvedSymbol = this@generateAccessorsByDelegate.symbol
        }
        typeRef = FirImplicitKPropertyTypeRef(null, ConeStarProjection)
    }

    delegateBuilder.delegateProvider = if (stubMode) buildExpressionStub() else buildFunctionCall {
        explicitReceiver = receiver
        calleeReference = buildSimpleNamedReference {
            source = null
            name = PROVIDE_DELEGATE
        }
        arguments += thisRef()
        arguments += propertyRef()
    }
    delegate = delegateBuilder.build()
    if (stubMode) return
    if (getter == null || getter is FirDefaultPropertyAccessor) {
        val returnTarget = FirFunctionTarget(null, isLambda = false)
        getter = buildPropertyAccessor {
            this.session = session
            returnTypeRef = buildImplicitTypeRef()
            isGetter = true
            status = FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)
            symbol = FirPropertyAccessorSymbol()

            body = FirSingleExpressionBlock(
                buildReturnExpression {
                    result = buildFunctionCall {
                        source = null
                        explicitReceiver = delegateAccess()
                        calleeReference = buildSimpleNamedReference {
                            source = null
                            name = GET_VALUE
                        }
                        arguments += thisRef()
                        arguments += propertyRef()
                    }
                    target = returnTarget
                }
            )
        }.also {
            returnTarget.bind(it)
        }
    }
    if (isVar && (setter == null || setter is FirDefaultPropertyAccessor)) {
        setter = buildPropertyAccessor {
            this.session = session
            returnTypeRef = session.builtinTypes.unitType
            isGetter = false
            status = FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)
            val parameter = buildValueParameter {
                this.session = session
                returnTypeRef = buildImplicitTypeRef()
                name = DELEGATED_SETTER_PARAM
                symbol = FirVariableSymbol(this@generateAccessorsByDelegate.name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
            }
            valueParameters += parameter
            symbol = FirPropertyAccessorSymbol()
            body = FirSingleExpressionBlock(
                buildFunctionCall {
                    explicitReceiver = delegateAccess()
                    calleeReference = buildSimpleNamedReference {
                        name = SET_VALUE
                    }
                    arguments += thisRef()
                    arguments += propertyRef()
                    arguments += buildQualifiedAccessExpression {
                        calleeReference = buildResolvedNamedReference {
                            name = DELEGATED_SETTER_PARAM
                            resolvedSymbol = parameter.symbol
                        }
                    }
                }
            )
        }
    }
}

fun FirTypeRef.convertToArrayType(): FirUserTypeRef = buildUserTypeRef {
    source = this@convertToArrayType.source
    isMarkedNullable = false
    qualifier += FirQualifierPartImpl(StandardClassIds.Array.shortClassName).apply {
        typeArguments += buildTypeProjectionWithVariance {
            source = this@convertToArrayType.source
            typeRef = this@convertToArrayType
            variance = Variance.OUT_VARIANCE
        }
    }
}

private val GET_VALUE = Name.identifier("getValue")
private val SET_VALUE = Name.identifier("setValue")
private val PROVIDE_DELEGATE = Name.identifier("provideDelegate")
private val DELEGATED_SETTER_PARAM = Name.special("<set-?>")
