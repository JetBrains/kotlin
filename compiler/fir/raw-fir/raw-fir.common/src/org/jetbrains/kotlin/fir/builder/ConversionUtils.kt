/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildDelegateFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
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
    other: FirExpression, baseSource: FirSourceElement?,
): FirElvisExpression {
    return buildElvisExpression {
        source = baseSource
        lhs = this@generateNotNullOrOther
        rhs = other
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
        source = baseSource?.fakeElement(FirFakeSourceElementKind.DesugaredInvertedContains)
        calleeReference = buildSimpleNamedReference {
            source = operationReferenceSource?.fakeElement(FirFakeSourceElementKind.DesugaredInvertedContains)
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

    val compareToCall = createConventionCall(
        operationReferenceSource,
        baseSource?.fakeElement(FirFakeSourceElementKind.GeneratedComparisonExpression),
        argument,
        OperatorNameConventions.COMPARE_TO
    )

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

fun generateAccessExpression(
    qualifiedSource: FirSourceElement?,
    calleeReferenceSource: FirSourceElement?,
    name: Name
): FirQualifiedAccessExpression =
    buildQualifiedAccessExpression {
        this.source = qualifiedSource
        calleeReference = buildSimpleNamedReference {
            this.source = calleeReferenceSource
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
        origin = FirDeclarationOrigin.Source
        returnTypeRef = typeRef ?: buildImplicitTypeRef {
            this.source = source
        }
        this.name = name
        this.initializer = initializer
        symbol = FirPropertySymbol(name)
        isVar = false
        isLocal = true
        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
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
        is FirRegularClassBuilder -> ownerClassBuilder.symbol
        else -> null
    }
    val isMember = ownerSymbol != null
    val fakeSource = delegateBuilder.source?.fakeElement(FirFakeSourceElementKind.DefaultAccessor)

    /*
     * If we have delegation with provide delegate then we generate call like
     *   `delegateExpression.provideDelegate(this, ::prop)`
     * Note that `this` is always  reference for dispatch receiver
     *   unlike other `this` references in `getValue` `setValue` calls, where
     *  `this` is reference to closest receiver (extension, then dispatch)
     *
     * So for top-level extension properties we should generate
     *   val A.prop by delegateExpression.provideDelegate(null, ::prop)
     *      get() = delegate.getValue(this@prop, ::prop)
     *
     * And for this case we can pass isForDelegateProviderCall to this reference
     *   generator function
     */
    fun thisRef(isForDelegateProviderCall: Boolean = false): FirExpression =
        when {
            ownerSymbol != null -> buildThisReceiverExpression {
                source = fakeSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = ownerSymbol
                }
                typeRef = buildResolvedTypeRef {
                    val typeParameterNumber = (ownerClassBuilder as? FirRegularClassBuilder)?.typeParameters?.size ?: 0
                    type = ownerSymbol.constructStarProjectedType(typeParameterNumber)
                }
            }
            isExtension && !isForDelegateProviderCall -> buildThisReceiverExpression {
                source = fakeSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = this@generateAccessorsByDelegate.symbol
                }
            }
            else -> buildConstExpression(null, FirConstKind.Null, null)
        }

    fun delegateAccess() = buildQualifiedAccessExpression {
        source = fakeSource
        calleeReference = buildDelegateFieldReference {
            resolvedSymbol = delegateFieldSymbol
        }
        if (ownerSymbol != null) {
            dispatchReceiver = thisRef()
        }
    }

    val isVar = this@generateAccessorsByDelegate.isVar
    fun propertyRef() = buildCallableReferenceAccess {
        source = fakeSource
        calleeReference = buildResolvedNamedReference {
            source = fakeSource
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
            source = fakeSource
            name = PROVIDE_DELEGATE
        }
        argumentList = buildBinaryArgumentList(thisRef(isForDelegateProviderCall = true), propertyRef())
    }
    delegate = delegateBuilder.build()
    if (stubMode) return
    if (getter == null || getter is FirDefaultPropertyAccessor) {
        val annotations = getter?.annotations
        val returnTarget = FirFunctionTarget(null, isLambda = false)
        getter = buildPropertyAccessor {
            this.source = fakeSource
            this.session = session
            origin = FirDeclarationOrigin.Source
            returnTypeRef = buildImplicitTypeRef()
            isGetter = true
            status = FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
            symbol = FirPropertyAccessorSymbol()

            body = FirSingleExpressionBlock(
                buildReturnExpression {
                    result = buildFunctionCall {
                        source = fakeSource
                        explicitReceiver = delegateAccess()
                        calleeReference = buildSimpleNamedReference {
                            source = fakeSource
                            name = GET_VALUE
                        }
                        argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
                    }
                    target = returnTarget
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
        }.also {
            returnTarget.bind(it)
        }
    }
    if (isVar && (setter == null || setter is FirDefaultPropertyAccessor)) {
        val annotations = setter?.annotations
        setter = buildPropertyAccessor {
            this.source = fakeSource
            this.session = session
            origin = FirDeclarationOrigin.Source
            returnTypeRef = session.builtinTypes.unitType
            isGetter = false
            status = FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
            val parameter = buildValueParameter {
                source = fakeSource
                this.session = session
                origin = FirDeclarationOrigin.Source
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
                    source = fakeSource
                    explicitReceiver = delegateAccess()
                    calleeReference = buildSimpleNamedReference {
                        name = SET_VALUE
                    }
                    argumentList = buildArgumentList {
                        arguments += thisRef()
                        arguments += propertyRef()
                        arguments += buildQualifiedAccessExpression {
                            calleeReference = buildResolvedNamedReference {
                                source = fakeSource
                                name = DELEGATED_SETTER_PARAM
                                resolvedSymbol = parameter.symbol
                            }
                        }
                    }
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
        }
    }
}

private val GET_VALUE = Name.identifier("getValue")
private val SET_VALUE = Name.identifier("setValue")
private val PROVIDE_DELEGATE = Name.identifier("provideDelegate")
private val DELEGATED_SETTER_PARAM = Name.special("<set-?>")

fun FirBlock?.extractContractDescriptionIfPossible(): Pair<FirBlock?, FirContractDescription?> {
    if (this == null) return null to null
    if (!isContractPresentFirCheck()) return this to null
    val contractCall = replaceFirstStatement(FirStubStatement) as FirFunctionCall
    return this to buildLegacyRawContractDescription {
        source = contractCall.source
        this.contractCall = contractCall
    }
}

fun FirBlock.isContractPresentFirCheck(): Boolean {
    val firstStatement = statements.firstOrNull() ?: return false
    val contractCall = firstStatement as? FirFunctionCall ?: return false
    if (contractCall.calleeReference.name.asString() != "contract") return false
    val receiver = contractCall.explicitReceiver as? FirQualifiedAccessExpression ?: return true
    if (!contractCall.checkReceiver("contracts")) return false
    if (!receiver.checkReceiver("kotlin")) return false
    val receiverOfReceiver = receiver.explicitReceiver as? FirQualifiedAccessExpression ?: return false
    if (receiverOfReceiver.explicitReceiver != null) return false
    return true
}

private fun FirExpression.checkReceiver(name: String?): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    val receiver = explicitReceiver as? FirQualifiedAccessExpression ?: return false
    val receiverName = (receiver.calleeReference as? FirNamedReference)?.name?.asString() ?: return false
    return receiverName == name
}


fun FirQualifiedAccess.wrapWithSafeCall(receiver: FirExpression): FirSafeCallExpression {
    val checkedSafeCallSubject = buildCheckedSafeCallSubject {
        @OptIn(FirContractViolation::class)
        this.originalReceiverRef = FirExpressionRef<FirExpression>().apply {
            bind(receiver)
        }
        this.source = receiver.source?.fakeElement(FirFakeSourceElementKind.CheckedSafeCallSubject)
    }

    replaceExplicitReceiver(checkedSafeCallSubject)
    return buildSafeCallExpression {
        this.receiver = receiver
        @OptIn(FirContractViolation::class)
        this.checkedSubjectRef = FirExpressionRef<FirCheckedSafeCallSubject>().apply {
            bind(checkedSafeCallSubject)
        }
        this.regularQualifiedAccess = this@wrapWithSafeCall
        this.source = this@wrapWithSafeCall.source?.fakeElement(FirFakeSourceElementKind.DesugaredSafeCallExpression)
    }
}

data class CalleeAndReceiver(
    val reference: FirNamedReference,
    val receiverExpression: FirExpression? = null,
    val isImplicitInvoke: Boolean = false
)
