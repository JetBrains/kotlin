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
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
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
                argumentList = buildBinaryArgumentList(
                    subjectExpression, buildConstExpression(baseSource, FirConstKind.Null, null)
                )
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

fun FirExpression.generateContainsOperation(
    argument: FirExpression,
    inverted: Boolean,
    baseSource: FirSourceElement?,
    operationReferenceSource: FirSourceElement?
): FirFunctionCall {
    val containsCall = createConventionCall(operationReferenceSource, baseSource, argument, OperatorNameConventions.CONTAINS)
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

fun FirExpression.generateComparisonExpression(
    argument: FirExpression,
    operatorToken: IElementType,
    baseSource: FirSourceElement?,
    operationReferenceSource: FirSourceElement?,
): FirComparisonExpression {
    require(operatorToken in OperatorConventions.COMPARISON_OPERATIONS) {
        "$operatorToken is not in ${OperatorConventions.COMPARISON_OPERATIONS}"
    }

    val compareToCall = createConventionCall(operationReferenceSource, baseSource, argument, OperatorNameConventions.COMPARE_TO)

    val firOperation = when (operatorToken) {
        KtTokens.LT -> FirOperation.LT
        KtTokens.GT -> FirOperation.GT
        KtTokens.LTEQ -> FirOperation.LT_EQ
        KtTokens.GTEQ -> FirOperation.GT_EQ
        else -> error("Unknown $operatorToken")
    }

    return buildComparisonExpression {
        this.source = baseSource
        this.operation = firOperation
        this.compareToCall = compareToCall
    }
}

private fun FirExpression.createConventionCall(
    operationReferenceSource: FirSourceElement?,
    baseSource: FirSourceElement?,
    argument: FirExpression,
    conventionName: Name
): FirFunctionCall {
    return buildFunctionCall {
        source = baseSource
        calleeReference = buildSimpleNamedReference {
            source = operationReferenceSource
            name = conventionName
        }
        explicitReceiver = this@createConventionCall
        argumentList = buildUnaryArgumentList(argument)
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
    ownerClassBuilder: FirClassBuilder?,
    session: FirSession,
    isExtension: Boolean,
    stubMode: Boolean,
    receiver: FirExpression?
) {
    if (delegateBuilder == null) return
    val delegateFieldSymbol = FirDelegateFieldSymbol<FirProperty>(symbol.callableId).also {
        this.delegateFieldSymbol = it
    }
    val ownerSymbol = when (ownerClassBuilder) {
        is FirAnonymousObjectBuilder -> ownerClassBuilder.symbol
        is AbstractFirRegularClassBuilder -> ownerClassBuilder.symbol
        else -> null
    }
    val isMember = ownerSymbol != null

    fun thisRef(): FirExpression =
        when {
            ownerSymbol != null -> buildThisReceiverExpression {
                source = delegateBuilder.source
                calleeReference = buildImplicitThisReference {
                    boundSymbol = ownerSymbol
                }
                typeRef = buildResolvedTypeRef {
                    val typeParameterNumber = (ownerClassBuilder as? AbstractFirRegularClassBuilder)?.typeParameters?.size ?: 0
                    type = ownerSymbol.constructStarProjectedType(typeParameterNumber)
                }
            }
            isExtension -> buildThisReceiverExpression {
                source = delegateBuilder.source
                calleeReference = buildImplicitThisReference {
                    boundSymbol = this@generateAccessorsByDelegate.symbol
                }
            }
            else -> buildConstExpression(null, FirConstKind.Null, null)
        }

    fun delegateAccess() = buildQualifiedAccessExpression {
        source = delegateBuilder.source
        calleeReference = buildDelegateFieldReference {
            resolvedSymbol = delegateFieldSymbol
        }
        if (ownerSymbol != null) {
            dispatchReceiver = thisRef()
        }
    }

    val isVar = this@generateAccessorsByDelegate.isVar
    fun propertyRef() = buildCallableReferenceAccess {
        source = delegateBuilder.source
        calleeReference = buildResolvedNamedReference {
            source = delegateBuilder.source
            name = this@generateAccessorsByDelegate.name
            resolvedSymbol = this@generateAccessorsByDelegate.symbol
        }
        typeRef = when {
            !isMember && !isExtension -> if (isVar) {
                FirImplicitKMutableProperty0TypeRef(null, ConeStarProjection)
            } else {
                FirImplicitKProperty0TypeRef(null, ConeStarProjection)
            }
            isMember && isExtension -> if (isVar) {
                FirImplicitKMutableProperty2TypeRef(null, ConeStarProjection, ConeStarProjection, ConeStarProjection)
            } else {
                FirImplicitKProperty2TypeRef(null, ConeStarProjection, ConeStarProjection, ConeStarProjection)
            }
            else -> if (isVar) {
                FirImplicitKMutableProperty1TypeRef(null, ConeStarProjection, ConeStarProjection)
            } else {
                FirImplicitKProperty1TypeRef(null, ConeStarProjection, ConeStarProjection)
            }
        }
    }

    delegateBuilder.delegateProvider = if (stubMode) buildExpressionStub() else buildFunctionCall {
        explicitReceiver = receiver
        calleeReference = buildSimpleNamedReference {
            source = delegateBuilder.source
            name = PROVIDE_DELEGATE
        }
        argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
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
                        source = delegateBuilder.source
                        explicitReceiver = delegateAccess()
                        calleeReference = buildSimpleNamedReference {
                            source = delegateBuilder.source
                            name = GET_VALUE
                        }
                        argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
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
                    argumentList = buildArgumentList {
                        arguments += thisRef()
                        arguments += propertyRef()
                        arguments += buildQualifiedAccessExpression {
                            calleeReference = buildResolvedNamedReference {
                                name = DELEGATED_SETTER_PARAM
                                resolvedSymbol = parameter.symbol
                            }
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
