/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
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
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

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
            return CharacterWithDiagnostic(DiagnosticKind.IllegalEscape)
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
    return CharacterWithDiagnostic(DiagnosticKind.IllegalEscape)
}

internal fun translateEscape(c: Char): CharacterWithDiagnostic =
    when (c) {
        't' -> CharacterWithDiagnostic('\t')
        'b' -> CharacterWithDiagnostic('\b')
        'n' -> CharacterWithDiagnostic('\n')
        'r' -> CharacterWithDiagnostic('\r')
        '\'' -> CharacterWithDiagnostic('\'')
        '\"' -> CharacterWithDiagnostic('\"')
        '\\' -> CharacterWithDiagnostic('\\')
        '$' -> CharacterWithDiagnostic('$')
        else -> CharacterWithDiagnostic(DiagnosticKind.IllegalEscape)
    }

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
    baseSource: KtSourceElement?,
    operationReferenceSource: KtSourceElement?
): FirFunctionCall {
    val containsCall = createConventionCall(operationReferenceSource, baseSource, argument, OperatorNameConventions.CONTAINS)
    if (!inverted) return containsCall

    return buildFunctionCall {
        source = baseSource?.fakeElement(KtFakeSourceElementKind.DesugaredInvertedContains)
        calleeReference = buildSimpleNamedReference {
            source = operationReferenceSource?.fakeElement(KtFakeSourceElementKind.DesugaredInvertedContains)
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
    diagnostic: ConeDiagnostic? = null
): FirQualifiedAccessExpression =
    buildPropertyAccessExpression {
        this.source = qualifiedSource
        calleeReference = buildSimpleNamedReference {
            this.source = if (calleeReferenceSource == qualifiedSource)
                calleeReferenceSource?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
            else
                calleeReferenceSource
            this.name = name
        }
        if (diagnostic != null) {
            this.nonFatalDiagnostics.add(diagnostic)
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

fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    name: Name,
    initializer: FirExpression,
    typeRef: FirTypeRef? = null,
    extractedAnnotations: Collection<FirAnnotation>? = null,
): FirVariable =
    buildProperty {
        this.source = source
        this.moduleData = moduleData
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
        if (extractedAnnotations != null) {
            // LT extracts annotations ahead.
            // PSI extracts annotations on demand. Use a similar util in [PsiConversionUtils]
            annotations.addAll(extractedAnnotations)
        }
    }

fun generateTemporaryVariable(
    moduleData: FirModuleData,
    source: KtSourceElement?,
    specialName: String,
    initializer: FirExpression,
    extractedAnnotations: Collection<FirAnnotation>? = null,
): FirVariable =
    generateTemporaryVariable(
        moduleData,
        source,
        Name.special("<$specialName>"),
        initializer,
        null,
        extractedAnnotations,
    )

val FirClassBuilder.ownerRegularOrAnonymousObjectSymbol
    get() = when (this) {
        is FirAnonymousObjectBuilder -> symbol
        is FirRegularClassBuilder -> symbol
        else -> null
    }

val FirClassBuilder.ownerRegularClassTypeParametersCount
    get() = if (this is FirRegularClassBuilder) typeParameters.size else null

fun <T> FirPropertyBuilder.generateAccessorsByDelegate(
    delegateBuilder: FirWrappedDelegateExpressionBuilder?,
    moduleData: FirModuleData,
    ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
    ownerRegularClassTypeParametersCount: Int?,
    context: Context<T>,
    isExtension: Boolean,
    receiver: FirExpression?
) {
    if (delegateBuilder == null) return
    val delegateFieldSymbol = FirDelegateFieldSymbol(symbol.callableId).also {
        this.delegateFieldSymbol = it
    }

    val isMember = ownerRegularOrAnonymousObjectSymbol != null
    val fakeSource = delegateBuilder.source?.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor)

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
    fun thisRef(forDispatchReceiver: Boolean = false): FirExpression =
        when {
            isExtension && !forDispatchReceiver -> buildThisReceiverExpression {
                source = fakeSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = this@generateAccessorsByDelegate.symbol
                }
            }
            ownerRegularOrAnonymousObjectSymbol != null -> buildThisReceiverExpression {
                source = fakeSource
                calleeReference = buildImplicitThisReference {
                    boundSymbol = ownerRegularOrAnonymousObjectSymbol
                }
                typeRef = buildResolvedTypeRef {
                    val typeParameterNumber = ownerRegularClassTypeParametersCount ?: 0
                    type = ownerRegularOrAnonymousObjectSymbol.constructStarProjectedType(typeParameterNumber)
                }
            }
            else -> buildConstExpression(null, ConstantValueKind.Null, null)
        }

    fun delegateAccess() = buildPropertyAccessExpression {
        source = fakeSource
        calleeReference = buildDelegateFieldReference {
            resolvedSymbol = delegateFieldSymbol
        }
        if (ownerRegularOrAnonymousObjectSymbol != null) {
            dispatchReceiver = thisRef(forDispatchReceiver = true)
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

    delegateBuilder.delegateProvider = buildFunctionCall {
        explicitReceiver = receiver
        calleeReference = buildSimpleNamedReference {
            source = fakeSource
            name = OperatorNameConventions.PROVIDE_DELEGATE
        }
        argumentList = buildBinaryArgumentList(thisRef(forDispatchReceiver = true), propertyRef())
        origin = FirFunctionCallOrigin.Operator
    }
    delegate = delegateBuilder.build()
    if (getter == null || getter is FirDefaultPropertyAccessor) {
        val annotations = getter?.annotations
        val returnTarget = FirFunctionTarget(null, isLambda = false)
        getter = buildPropertyAccessor {
            this.source = fakeSource
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = buildImplicitTypeRef()
            isGetter = true
            status = FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL).apply {
                isInline = this@generateAccessorsByDelegate.getter?.status?.isInline ?: isInline
            }
            symbol = FirPropertyAccessorSymbol()

            body = FirSingleExpressionBlock(
                buildReturnExpression {
                    result = buildFunctionCall {
                        source = fakeSource
                        explicitReceiver = delegateAccess()
                        calleeReference = buildSimpleNamedReference {
                            source = fakeSource
                            name = OperatorNameConventions.GET_VALUE
                        }
                        argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
                        origin = FirFunctionCallOrigin.Operator
                    }
                    target = returnTarget
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
            propertySymbol = this@generateAccessorsByDelegate.symbol
        }.also {
            returnTarget.bind(it)
            it.initContainingClassAttr(context)
        }
    }
    if (isVar && (setter == null || setter is FirDefaultPropertyAccessor)) {
        val annotations = setter?.annotations
        val parameterAnnotations = setter?.valueParameters?.firstOrNull()?.annotations
        setter = buildPropertyAccessor {
            this.source = fakeSource
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = moduleData.session.builtinTypes.unitType
            isGetter = false
            status = FirDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL).apply {
                isInline = this@generateAccessorsByDelegate.setter?.status?.isInline ?: isInline
            }
            val parameter = buildValueParameter {
                source = fakeSource
                this.moduleData = moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = buildImplicitTypeRef()
                name = SpecialNames.IMPLICIT_SET_PARAMETER
                symbol = FirValueParameterSymbol(this@generateAccessorsByDelegate.name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
                if (parameterAnnotations != null) {
                    this.annotations.addAll(parameterAnnotations)
                }
            }
            valueParameters += parameter
            symbol = FirPropertyAccessorSymbol()
            body = FirSingleExpressionBlock(
                buildFunctionCall {
                    source = fakeSource
                    explicitReceiver = delegateAccess()
                    calleeReference = buildSimpleNamedReference {
                        source = fakeSource
                        name = OperatorNameConventions.SET_VALUE
                    }
                    argumentList = buildArgumentList {
                        arguments += thisRef()
                        arguments += propertyRef()
                        arguments += buildPropertyAccessExpression {
                            calleeReference = buildResolvedNamedReference {
                                source = fakeSource
                                name = SpecialNames.IMPLICIT_SET_PARAMETER
                                resolvedSymbol = parameter.symbol
                            }
                        }
                    }
                    origin = FirFunctionCallOrigin.Operator
                }
            )
            if (annotations != null) {
                this.annotations.addAll(annotations)
            }
            propertySymbol = this@generateAccessorsByDelegate.symbol
        }.also {
            it.initContainingClassAttr(context)
        }
    }
}

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

fun FirQualifiedAccess.wrapWithSafeCall(receiver: FirExpression, source: KtSourceElement): FirSafeCallExpression {
    val checkedSafeCallSubject = buildCheckedSafeCallSubject {
        @OptIn(FirContractViolation::class)
        this.originalReceiverRef = FirExpressionRef<FirExpression>().apply {
            bind(receiver)
        }
        this.source = receiver.source?.fakeElement(KtFakeSourceElementKind.CheckedSafeCallSubject)
    }

    replaceExplicitReceiver(checkedSafeCallSubject)
    return buildSafeCallExpression {
        this.receiver = receiver
        @OptIn(FirContractViolation::class)
        this.checkedSubjectRef = FirExpressionRef<FirCheckedSafeCallSubject>().apply {
            bind(checkedSafeCallSubject)
        }
        this.regularQualifiedAccess = this@wrapWithSafeCall
        this.source = source
    }
}

fun List<FirAnnotationCall>.filterUseSiteTarget(target: AnnotationUseSiteTarget): List<FirAnnotationCall> =
    mapNotNull {
        if (it.useSiteTarget != target) null
        else buildAnnotationCall {
            source = it.source?.fakeElement(KtFakeSourceElementKind.FromUseSiteTarget)
            useSiteTarget = it.useSiteTarget
            annotationTypeRef = it.annotationTypeRef
            argumentList = it.argumentList
            calleeReference = it.calleeReference
            argumentMapping = it.argumentMapping
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
    val receiverExpression: FirExpression? = null,
    val isImplicitInvoke: Boolean = false
)
