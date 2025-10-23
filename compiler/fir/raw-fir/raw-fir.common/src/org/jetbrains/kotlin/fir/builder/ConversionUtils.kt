/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildDelegateFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.symbolIdFactory
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun String.parseCharacter(): CharacterWithDiagnostic {
    // Strip the quotes
    if (length < 2 || this[0] != '\'' || this[length - 1] != '\'') {
        return CharacterWithDiagnostic(DiagnosticKind.IncorrectCharacterLiteral)
    }
    val text = substring(1, length - 1) // now there is no quotes

    if (text.isEmpty()) {
        return CharacterWithDiagnostic(DiagnosticKind.EmptyCharacterLiteral)
    }

    return if (text[0] != '\\') {
        // No escape
        if (text.length == 1) {
            CharacterWithDiagnostic(text[0])
        } else {
            CharacterWithDiagnostic(DiagnosticKind.TooManyCharactersInCharacterLiteral)
        }
    } else {
        escapedStringToCharacter(text)
    }
}

fun escapedStringToCharacter(text: String): CharacterWithDiagnostic {
    assert(text.isNotEmpty() && text[0] == '\\') {
        "Only escaped sequences must be passed to this routine: $text"
    }

    // Escape
    val escape = text.substring(1) // strip the slash
    when (escape.length) {
        0 -> {
            // bare slash
            return illegalEscapeDiagnostic
        }
        1 -> {
            // one-char escape
            return translateEscape(escape[0])
        }
        5 -> {
            // unicode escape
            if (escape[0] == 'u') {
                val intValue = escape.substring(1).toIntOrNull(16)
                // If error occurs it will be reported below
                if (intValue != null) {
                    return CharacterWithDiagnostic(intValue.toChar())
                }
            }
        }
    }
    return illegalEscapeDiagnostic
}

internal fun translateEscape(c: Char): CharacterWithDiagnostic = escapeCharToChartedWithDiagnosticMap[c] ?: illegalEscapeDiagnostic

private val escapeCharToChartedWithDiagnosticMap = hashMapOf(
    't' to CharacterWithDiagnostic('\t'),
    'b' to CharacterWithDiagnostic('\b'),
    'n' to CharacterWithDiagnostic('\n'),
    'r' to CharacterWithDiagnostic('\r'),
    '\'' to CharacterWithDiagnostic('\''),
    '\"' to CharacterWithDiagnostic('\"'),
    '\\' to CharacterWithDiagnostic('\\'),
    '$' to CharacterWithDiagnostic('$'),
)

private val illegalEscapeDiagnostic = CharacterWithDiagnostic(DiagnosticKind.IllegalEscape)

class CharacterWithDiagnostic {
    private val diagnostic: DiagnosticKind?
    val value: Char?

    constructor(diagnostic: DiagnosticKind) {
        this.diagnostic = diagnostic
        this.value = null
    }

    constructor(value: Char) {
        this.diagnostic = null
        this.value = value
    }

    fun getDiagnostic(): DiagnosticKind? {
        return diagnostic
    }
}

fun IElementType.toBinaryName(): Name? {
    return OperatorConventions.BINARY_OPERATION_NAMES[this]
}

fun IElementType.toUnaryName(): Name? {
    return OperatorConventions.UNARY_OPERATION_NAMES[this]
}

fun IElementType.toFirOperation(): FirOperation =
    toFirOperationOrNull() ?: error("Cannot convert element type to FIR operation: $this")

fun IElementType.toFirOperationOrNull(): FirOperation? = ktTokenToFirOperationMap[this]

private val ktTokenToFirOperationMap = hashMapOf(
    KtTokens.LT to FirOperation.LT,
    KtTokens.GT to FirOperation.GT,
    KtTokens.LTEQ to FirOperation.LT_EQ,
    KtTokens.GTEQ to FirOperation.GT_EQ,
    KtTokens.EQEQ to FirOperation.EQ,
    KtTokens.EXCLEQ to FirOperation.NOT_EQ,
    KtTokens.EQEQEQ to FirOperation.IDENTITY,
    KtTokens.EXCLEQEQEQ to FirOperation.NOT_IDENTITY,

    KtTokens.EQ to FirOperation.ASSIGN,
    KtTokens.PLUSEQ to FirOperation.PLUS_ASSIGN,
    KtTokens.MINUSEQ to FirOperation.MINUS_ASSIGN,
    KtTokens.MULTEQ to FirOperation.TIMES_ASSIGN,
    KtTokens.DIVEQ to FirOperation.DIV_ASSIGN,
    KtTokens.PERCEQ to FirOperation.REM_ASSIGN,

    KtTokens.IS_KEYWORD to FirOperation.IS,
    KtTokens.NOT_IS to FirOperation.NOT_IS,

    KtTokens.AS_KEYWORD to FirOperation.AS,
    KtTokens.AS_SAFE to FirOperation.SAFE_AS,
)

fun FirExpression.generateNotNullOrOther(
    other: FirExpression, baseSource: KtSourceElement?,
): FirElvisExpression {
    return buildElvisExpression {
        source = baseSource
        lhs = this@generateNotNullOrOther
        rhs = other
    }
}

fun FirExpression.generateLazyLogicalOperation(
    other: FirExpression, isAnd: Boolean, baseSource: KtSourceElement?,
): FirBooleanOperatorExpression {
    return buildBooleanOperatorExpression {
        source = baseSource
        leftOperand = this@generateLazyLogicalOperation
        rightOperand = other
        kind = if (isAnd) LogicOperationKind.AND else LogicOperationKind.OR
    }
}

fun FirExpression.generateContainsOperation(
    argument: FirExpression,
    inverted: Boolean,
    baseSource: KtSourceElement?,
    operationReferenceSource: KtSourceElement?
): FirFunctionCall {
    val resultReferenceSource = if (inverted)
        operationReferenceSource?.fakeElement(KtFakeSourceElementKind.DesugaredInvertedContains)
    else
        operationReferenceSource
    val containsCall = createConventionCall(resultReferenceSource, baseSource, argument, OperatorNameConventions.CONTAINS)
    if (!inverted) return containsCall

    return buildFunctionCall {
        source = baseSource?.fakeElement(KtFakeSourceElementKind.DesugaredInvertedContains)
        calleeReference = buildSimpleNamedReference {
            source = resultReferenceSource
            name = OperatorNameConventions.NOT
        }
        explicitReceiver = containsCall
        origin = FirFunctionCallOrigin.Operator
    }
}

fun FirExpression.generateComparisonExpression(
    argument: FirExpression,
    operatorToken: IElementType,
    baseSource: KtSourceElement?,
    operationReferenceSource: KtSourceElement?,
): FirComparisonExpression {
    require(operatorToken in OperatorConventions.COMPARISON_OPERATIONS) {
        "$operatorToken is not in ${OperatorConventions.COMPARISON_OPERATIONS}"
    }

    val compareToCall = createConventionCall(
        operationReferenceSource,
        baseSource?.fakeElement(KtFakeSourceElementKind.GeneratedComparisonExpression),
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
    operationReferenceSource: KtSourceElement?,
    baseSource: KtSourceElement?,
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
        origin = FirFunctionCallOrigin.Operator
    }
}

fun generateAccessExpression(
    qualifiedSource: KtSourceElement?,
    calleeReferenceSource: KtSourceElement?,
    name: Name,
): FirPropertyAccessExpression = buildPropertyAccessExpression {
    this.source = qualifiedSource
    calleeReference = buildSimpleNamedReference {
        this.source = if (calleeReferenceSource == qualifiedSource)
            calleeReferenceSource?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
        else
            calleeReferenceSource
        this.name = name
    }
}

fun generateResolvedAccessExpression(source: KtSourceElement?, variable: FirVariable): FirQualifiedAccessExpression =
    buildPropertyAccessExpression {
        this.source = source
        calleeReference = buildResolvedNamedReference {
            this.source = source
            name = variable.name
            resolvedSymbol = variable.symbol
        }
    }

fun FirVariable.toComponentCall(
    entrySource: KtSourceElement?,
    index: Int,
): FirComponentCall {
    return buildComponentCall {
        val componentCallSource = entrySource?.fakeElement(KtFakeSourceElementKind.DesugaredComponentFunctionCall)
        source = componentCallSource
        explicitReceiver = generateResolvedAccessExpression(componentCallSource, this@toComponentCall)
        componentIndex = index + 1
    }
}

val FirClassBuilder.ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<FirClass>
    get() = when (this) {
        is FirAnonymousObjectBuilder -> symbol
        is FirRegularClassBuilder -> symbol
    }

/**
 * Operates on an already configured [FirPropertyBuilder] to convert the property to a delegated property with a delegate expression (built
 * by [delegateBuilder]), a getter, and a setter.
 *
 * When building a getter and setter, the function takes the previous getter and setter into account.
 */
fun <T> FirPropertyBuilder.generateAccessorsByDelegate(
    delegateBuilder: FirWrappedDelegateExpressionBuilder?,
    delegateSource: KtSourceElement,
    moduleData: FirModuleData,
    ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
    context: Context<T>,
    isExtension: Boolean,
    lazyDelegateExpression: FirLazyExpression? = null,
    lazyBodyForGeneratedAccessors: FirLazyBlock? = null,
    bindFunction: (target: FirFunctionTarget, function: FirFunction) -> Unit = FirFunctionTarget::bind,
    explicitDeclarationSource: KtSourceElement? = null
) {
    if (delegateBuilder == null) return

    val symbolIdFactory = moduleData.session.symbolIdFactory
    val declarationSource = explicitDeclarationSource ?: delegateSource

    val delegateFieldSource =
        declarationSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertyDelegateField)

    val delegateFieldSymbol = FirDelegateFieldSymbol(symbolIdFactory.sourceBased(delegateFieldSource), symbol).also {
        this.delegateFieldSymbol = it
    }

    val isMember = ownerRegularOrAnonymousObjectSymbol != null

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
    fun thisRef(thisReferenceSource: KtSourceElement?, forDispatchReceiver: Boolean = false): FirExpression =
        when {
            isExtension && !forDispatchReceiver -> buildThisReceiverExpression {
                source = thisReferenceSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = this@generateAccessorsByDelegate.receiverParameter?.symbol
                }
            }
            ownerRegularOrAnonymousObjectSymbol != null -> buildThisReceiverExpression {
                source = thisReferenceSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = ownerRegularOrAnonymousObjectSymbol
                }
                coneTypeOrNull = context.dispatchReceiverTypesStack.last()
            }
            else -> buildLiteralExpression(null, ConstantValueKind.Null, null, setType = false)
        }

    fun delegateAccess(delegateAccessSource: KtSourceElement?) = buildPropertyAccessExpression {
        source = delegateAccessSource
        calleeReference = buildDelegateFieldReference {
            source = delegateAccessSource
            resolvedSymbol = delegateFieldSymbol
        }
        if (ownerRegularOrAnonymousObjectSymbol != null) {
            dispatchReceiver = thisRef(delegateAccessSource, forDispatchReceiver = true)
        }
    }

    val isVar = this@generateAccessorsByDelegate.isVar
    fun propertyRef(propertyReferenceSource: KtSourceElement?) = buildCallableReferenceAccess {
        source = propertyReferenceSource
        calleeReference = buildResolvedNamedReference {
            source = propertyReferenceSource
            name = this@generateAccessorsByDelegate.name
            resolvedSymbol = this@generateAccessorsByDelegate.symbol
        }
        coneTypeOrNull = when {
            !isMember && !isExtension -> if (isVar) {
                StandardClassIds.KMutableProperty0.constructClassLikeType(arrayOf(ConeStarProjection))
            } else {
                StandardClassIds.KProperty0.constructClassLikeType(arrayOf(ConeStarProjection))
            }
            isMember && isExtension -> if (isVar) {
                StandardClassIds.KMutableProperty2.constructClassLikeType(
                    arrayOf(
                        ConeStarProjection,
                        ConeStarProjection,
                        ConeStarProjection
                    )
                )
            } else {
                StandardClassIds.KProperty2.constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection, ConeStarProjection))
            }
            else -> if (isVar) {
                StandardClassIds.KMutableProperty1.constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection))
            } else {
                StandardClassIds.KProperty1.constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection))
            }
        }
        this@generateAccessorsByDelegate.typeParameters.mapTo(typeArguments) {
            buildTypeProjectionWithVariance {
                source = propertyReferenceSource
                variance = Variance.INVARIANT
                typeRef = buildResolvedTypeRef {
                    coneType = ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
                    source = propertyReferenceSource
                }
            }
        }
    }

    val delegateCallSource =
        delegateSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertyDelegateExpression)

    delegate = lazyDelegateExpression ?: run {
        delegateBuilder.provideDelegateCall = buildFunctionCall {
            explicitReceiver = delegateBuilder.expression
            calleeReference = buildSimpleNamedReference {
                source = delegateCallSource
                name = OperatorNameConventions.PROVIDE_DELEGATE
            }
            argumentList = buildBinaryArgumentList(thisRef(delegateCallSource, forDispatchReceiver = true), propertyRef(delegateCallSource))
            origin = FirFunctionCallOrigin.Operator
            source = delegateCallSource
        }

        delegateBuilder.build()
    }

    if (getter == null || getter is FirDefaultPropertyAccessor) {
        val annotations = getter?.annotations
        val returnTarget = FirFunctionTarget(null, isLambda = false)
        val getterStatus = getter?.status

        val getterElement = getter?.source?.takeIf { it.kind == KtRealSourceElementKind }
            ?: declarationSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertyGetter)

        // We take the delegate source instead of the getter source as we want to report issues with the getter on the delegate call
        // expression, not the whole property, since the delegate call "generates" the getter. For example, reporting
        // `CANNOT_INFER_PARAMETER_TYPE` on `A()` in `val p by A()`.
        val bodyFakeSource = delegateSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertyGetter)

        getter = buildPropertyAccessor {
            this.source = getterElement
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = FirImplicitTypeRefImplWithoutSource
            isGetter = true
            status = FirDeclarationStatusImpl(getterStatus?.visibility ?: Visibilities.Unknown, Modality.FINAL).apply {
                isInline = getterStatus?.isInline ?: isInline
            }
            symbol = FirPropertyAccessorSymbol(symbolIdFactory.sourceBased(getterElement))
            body = lazyBodyForGeneratedAccessors ?: FirSingleExpressionBlock(
                buildReturnExpression {
                    result = buildFunctionCall {
                        source = bodyFakeSource
                        explicitReceiver = delegateAccess(bodyFakeSource)
                        calleeReference = buildSimpleNamedReference {
                            source = bodyFakeSource
                            name = OperatorNameConventions.GET_VALUE
                        }
                        argumentList = buildBinaryArgumentList(thisRef(bodyFakeSource), propertyRef(bodyFakeSource))
                        origin = FirFunctionCallOrigin.Operator
                    }
                    target = returnTarget
                    source = bodyFakeSource
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
            propertySymbol = this@generateAccessorsByDelegate.symbol
        }.also {
            bindFunction(returnTarget, it)
            it.initContainingClassAttr(context)
        }
    }

    if (isVar && (setter == null || setter is FirDefaultPropertyAccessor)) {
        val annotations = setter?.annotations
        val returnTarget = FirFunctionTarget(null, isLambda = false)
        val parameterAnnotations = setter?.valueParameters?.firstOrNull()?.annotations
        val setterStatus = setter?.status

        val setterElement = setter?.source?.takeIf { it.kind is KtRealSourceElementKind }
            ?: declarationSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertySetter)

        // We take the delegate source instead of the setter source as we want to report issues with the setter on the delegate call
        // expression, not the whole property, since the delegate call "generates" the setter. For example, reporting
        // `CANNOT_INFER_PARAMETER_TYPE` on `A()` in `val p by A()`.
        val bodyFakeSource = delegateSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertySetter)

        setter = buildPropertyAccessor {
            this.source = setterElement
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = moduleData.session.builtinTypes.unitType
            isGetter = false
            status = FirDeclarationStatusImpl(setterStatus?.visibility ?: Visibilities.Unknown, Modality.FINAL).apply {
                isInline = setterStatus?.isInline ?: isInline
            }
            symbol = FirPropertyAccessorSymbol(symbolIdFactory.sourceBased(setterElement))

            val parameterSource =
                bodyFakeSource.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor.DelegatedPropertySetterValueParameter)

            val parameter = buildValueParameter {
                source = parameterSource
                containingDeclarationSymbol = this@buildPropertyAccessor.symbol
                this.moduleData = moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = FirImplicitTypeRefImplWithoutSource
                name = SpecialNames.IMPLICIT_SET_PARAMETER
                symbol = FirValueParameterSymbol(symbolIdFactory.sourceBased(parameterSource))
                isCrossinline = false
                isNoinline = false
                isVararg = false
                if (parameterAnnotations != null) {
                    this.annotations.addAll(parameterAnnotations)
                }
            }
            valueParameters += parameter
            body = lazyBodyForGeneratedAccessors ?: FirSingleExpressionBlock(
                buildReturnExpression {
                    result = buildFunctionCall {
                        source = bodyFakeSource
                        explicitReceiver = delegateAccess(bodyFakeSource)
                        calleeReference = buildSimpleNamedReference {
                            source = bodyFakeSource
                            name = OperatorNameConventions.SET_VALUE
                        }
                        argumentList = buildArgumentList {
                            arguments += thisRef(bodyFakeSource)
                            arguments += propertyRef(bodyFakeSource)
                            arguments += buildPropertyAccessExpression {
                                source = bodyFakeSource
                                calleeReference = buildResolvedNamedReference {
                                    source = bodyFakeSource
                                    name = SpecialNames.IMPLICIT_SET_PARAMETER
                                    resolvedSymbol = parameter.symbol
                                }
                            }
                        }
                        origin = FirFunctionCallOrigin.Operator
                    }
                    target = returnTarget
                    source = bodyFakeSource
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
            propertySymbol = this@generateAccessorsByDelegate.symbol
        }.also {
            bindFunction(returnTarget, it)
            it.initContainingClassAttr(context)
        }
    }
}

fun processLegacyContractDescription(block: FirBlock, diagnostic: ConeDiagnostic?): FirContractDescription? {
    if (block.isContractPresentFirCheck()) {
        val contractCall = block.replaceFirstStatement<FirFunctionCall> { FirContractCallBlock(it) }
        return contractCall.toLegacyRawContractDescription(diagnostic)
    }

    return null
}

fun FirFunctionCall.toLegacyRawContractDescription(diagnostic: ConeDiagnostic? = null): FirLegacyRawContractDescription {
    return buildLegacyRawContractDescription {
        this.source = this@toLegacyRawContractDescription.source
        this.contractCall = this@toLegacyRawContractDescription
        this.diagnostic = diagnostic
    }
}

fun FirBlock.isContractPresentFirCheck(): Boolean {
    val firstStatement = statements.firstOrNull() ?: return false
    return firstStatement.isContractBlockFirCheck()
}

@OptIn(ExperimentalContracts::class)
fun FirStatement.isContractBlockFirCheck(): Boolean {
    contract { returns(true) implies (this@isContractBlockFirCheck is FirFunctionCall) }

    val contractCall = this as? FirFunctionCall ?: return false
    if (contractCall.calleeReference.name.asString() != "contract") return false
    if (contractCall.arguments.singleOrNull()?.unwrapArgument() !is FirAnonymousFunctionExpression) return false
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

// this = .f(...)
// receiver = <expr>
// Returns safe call <expr>?.{ f(...) }
fun FirQualifiedAccessExpression.createSafeCall(receiver: FirExpression, source: KtSourceElement): FirSafeCallExpression {
    val checkedSafeCallSubject = buildCheckedSafeCallSubject {
        @OptIn(FirContractViolation::class)
        this.originalReceiverRef = FirExpressionRef<FirExpression>().apply {
            bind(receiver)
        }
        this.source = receiver.source?.fakeElement(KtFakeSourceElementKind.CheckedSafeCallSubject)
    }
    // If an `invoke` function from a functional type expects a
    // receiver, it's still defined as the first value parameter.
    // A construction like `1.(fun Int.() = 1)()` means we're calling
    // `Function1<Int, Unit>.invoke(Int)`.
    if (this is FirImplicitInvokeCall) {
        val newArguments = buildArgumentList {
            arguments.add(checkedSafeCallSubject)
            arguments.addAll(this@createSafeCall.arguments)
        }
        replaceArgumentList(newArguments)
    } else {
        replaceExplicitReceiver(checkedSafeCallSubject)
    }
    return buildSafeCallExpression {
        this.receiver = receiver
        @OptIn(FirContractViolation::class)
        this.checkedSubjectRef = FirExpressionRef<FirCheckedSafeCallSubject>().apply {
            bind(checkedSafeCallSubject)
        }
        this.selector = this@createSafeCall
        this.source = source
    }
}

fun FirQualifiedAccessExpression.pullUpSafeCallIfNecessary(): FirExpression =
    pullUpSafeCallIfNecessary(
        FirQualifiedAccessExpression::explicitReceiver,
        FirQualifiedAccessExpression::replaceExplicitReceiver
    )

// Turns a?.b.f(...) to a?.{ b.f(...) ) -- for any qualified access `.f(...)`
// Other patterns remain unchanged
fun <F : FirExpression> F.pullUpSafeCallIfNecessary(
    obtainReceiver: F.() -> FirExpression?,
    replaceReceiver: F.(FirExpression) -> Unit,
): FirExpression {
    val safeCall = obtainReceiver() as? FirSafeCallExpression ?: return this
    val safeCallSelector = safeCall.selector as? FirExpression ?: return this

    // (a?.b).f and `(a?.b)[3]` should be left as is
    if (safeCall.isChildInParentheses()) return this

    replaceReceiver(safeCallSelector)
    safeCall.replaceSelector(this)

    return safeCall
}

fun FirStatement.isChildInParentheses(): Boolean {
    val sourceElement = source ?: error("Nullable source")
    return sourceElement.isChildInParentheses()
}

fun KtSourceElement.isChildInParentheses(): Boolean =
    treeStructure.getParent(lighterASTNode)?.tokenType == KtNodeTypes.PARENTHESIZED

fun List<FirAnnotationCall>.filterUseSiteTarget(target: AnnotationUseSiteTarget): List<FirAnnotationCall> =
    mapNotNull {
        if (it.useSiteTarget != target) null
        else buildAnnotationCallCopy(it) {
            source = it.source?.fakeElement(KtFakeSourceElementKind.FromUseSiteTarget)
        }
    }

fun AbstractRawFirBuilder<*>.createReceiverParameter(
    symbolId: FirSymbolId<FirReceiverParameterSymbol>,
    typeRefCalculator: () -> FirTypeRef,
    moduleData: FirModuleData,
    containingCallableSymbol: FirCallableSymbol<*>,
): FirReceiverParameter = buildReceiverParameter {
    // We cannot create the symbol ID from the source of the calculated `FirTypeRef` because we need the symbol ID to build the symbol,
    // which in turn is needed as a container symbol for the type ref calculation. Hence, the best approach is to explicitly require passing
    // in a symbol ID.
    symbol = FirReceiverParameterSymbol(symbolId)
    withContainerSymbol(symbol) {
        // `typeRefCalculator` is nested in `withContainerSymbol` on purpose to provide the correct container symbol context during type ref
        // calculation.
        val typeRef = typeRefCalculator()
        source = typeRef.source?.fakeElement(KtFakeSourceElementKind.ReceiverFromType)

        @Suppress("UNCHECKED_CAST")
        annotations += (typeRef.annotations as List<FirAnnotationCall>).filterUseSiteTarget(AnnotationUseSiteTarget.RECEIVER)
        val filteredTypeRefAnnotations = typeRef.annotations.filterNot { it.useSiteTarget == AnnotationUseSiteTarget.RECEIVER }
        if (filteredTypeRefAnnotations.size != typeRef.annotations.size) {
            typeRef.replaceAnnotations(filteredTypeRefAnnotations)
        }

        this.typeRef = typeRef
        this.moduleData = moduleData
        origin = FirDeclarationOrigin.Source
        this.containingDeclarationSymbol = containingCallableSymbol
    }
}

fun KtSourceElement.asReceiverParameter(
    moduleData: FirModuleData,
    containingCallableSymbol: FirCallableSymbol<*>,
): FirReceiverParameter {
    val fakeElement = this@asReceiverParameter.fakeElement(KtFakeSourceElementKind.ReceiverFromType)
    return buildReceiverParameter {
        source = fakeElement
        typeRef = FirImplicitTypeRefImplWithoutSource
        symbol = FirReceiverParameterSymbol(moduleData.session.symbolIdFactory.sourceBased(fakeElement))
        this.moduleData = moduleData
        origin = FirDeclarationOrigin.Source
        this.containingDeclarationSymbol = containingCallableSymbol
    }
}

fun <T> FirCallableDeclaration.initContainingClassAttr(context: Context<T>) {
    containingClassForStaticMemberAttr = currentDispatchReceiverType(context)?.lookupTag ?: return
}

fun <T> currentDispatchReceiverType(context: Context<T>): ConeClassLikeType? {
    return context.dispatchReceiverTypesStack.lastOrNull()
}

val CharSequence.isUnderscore: Boolean
    get() = all { it == '_' }

data class CalleeAndReceiver(
    val reference: FirNamedReference,
    val receiverForInvoke: FirExpression? = null,
)

/**
 * Creates balanced tree of OR expressions for given set of conditions
 * We do so, to avoid too deep OR-expression structures, that can cause running out of stack while processing
 * [conditions] should contain at least one element, otherwise it will cause StackOverflow
 */
fun buildBalancedOrExpressionTree(conditions: List<FirExpression>, lower: Int = 0, upper: Int = conditions.lastIndex): FirExpression {
    val size = upper - lower + 1
    val middle = size / 2 + lower

    if (lower == upper) {
        return conditions[middle]
    }
    val leftNode = buildBalancedOrExpressionTree(conditions, lower, middle - 1)
    val rightNode = buildBalancedOrExpressionTree(conditions, middle, upper)

    return leftNode.generateLazyLogicalOperation(
        rightNode,
        isAnd = false,
        (leftNode.source ?: rightNode.source)?.fakeElement(KtFakeSourceElementKind.WhenCondition)
    )
}

fun FirExpression.guardedBy(
    guard: FirExpression?,
): FirExpression = when (guard) {
    null -> this
    else -> this.generateLazyLogicalOperation(
        guard,
        isAnd = true,
        (this.source ?: guard.source)?.fakeElement(KtFakeSourceElementKind.WhenCondition)
    )
}

fun AnnotationUseSiteTarget?.appliesToPrimaryConstructorParameter(): Boolean = this == null ||
        this == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER ||
        this == AnnotationUseSiteTarget.RECEIVER ||
        this == AnnotationUseSiteTarget.FILE ||
        this == AnnotationUseSiteTarget.ALL

fun FirErrorTypeRef.wrapIntoArray(): FirResolvedTypeRef {
    val typeRef = this
    return buildResolvedTypeRef {
        source = typeRef.source
        coneType = StandardClassIds.Array.constructClassLikeType(arrayOf(ConeKotlinTypeProjectionOut(typeRef.coneType)))
        delegatedTypeRef = typeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ArrayTypeFromVarargParameter)
    }
}

fun shouldGenerateDelegatedSuperCall(
    isAnySuperCall: Boolean,
    isExpectClass: Boolean,
    isEnumEntry: Boolean,
    hasExplicitDelegatedCalls: Boolean
): Boolean {
    if (isAnySuperCall) {
        return false
    }

    if (isExpectClass) {
        // Generally, an `expect` class cannot inherit from other expect class.
        // However, for the IDE resolution purposes, we keep invalid explicit delegate calls.
        return !isEnumEntry && hasExplicitDelegatedCalls
    }

    return true
}
