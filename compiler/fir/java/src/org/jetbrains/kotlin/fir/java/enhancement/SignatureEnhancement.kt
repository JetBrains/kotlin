/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.unexpandedClassId
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.DeferredCallableCopyReturnType
import org.jetbrains.kotlin.fir.scopes.deferredCallableCopyReturnType
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType.VALUE_PARAMETER
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class FirSignatureEnhancement(
    private val owner: FirRegularClass,
    private val session: FirSession,
    private val overridden: FirCallableDeclaration.() -> List<FirCallableDeclaration>,
) {
    /*
     * FirSignatureEnhancement may be created with library session which doesn't have single module data,
     *   so owner is a only place where module data can be obtained. However it's guaranteed that `owner`
     *   was created for same session as one passed to constructor, so it's safe to use owners module data
     */
    private val moduleData get() = owner.moduleData

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (owner is FirJavaClass) owner.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private val typeQualifierResolver = session.javaAnnotationTypeQualifierResolver

    // This property is assumed to be initialized only after annotations for the class are initialized
    // While in one of the cases FirSignatureEnhancement is created just one step before annotations resolution
    private val contextQualifiers: JavaTypeQualifiersByElementType? by lazy(LazyThreadSafetyMode.NONE) {
        typeQualifierResolver.extractDefaultQualifiers(owner)
    }

    private val enhancementsCache = session.enhancedSymbolStorage.cacheByOwner.getValue(owner.symbol, null)

    fun enhancedFunction(
        function: FirNamedFunctionSymbol,
        name: Name,
        precomputedOverridden: List<FirCallableDeclaration>? = null,
    ): FirNamedFunctionSymbol {
        return enhancedFunctionImpl(function, name, precomputedOverridden) as FirNamedFunctionSymbol
    }

    fun enhancedConstructor(constructor: FirConstructorSymbol): FirConstructorSymbol {
        return enhancedFunctionImpl(constructor, name = null, precomputedOverridden = null) as FirConstructorSymbol
    }

    private fun enhancedFunctionImpl(
        function: FirFunctionSymbol<*>,
        name: Name?,
        precomputedOverridden: List<FirCallableDeclaration>? = null,
    ): FirFunctionSymbol<*> {
        return enhancementsCache.enhancedFunctions.getValue(
            function,
            FirEnhancedSymbolsStorage.FunctionEnhancementContext(this, name, precomputedOverridden)
        )
    }

    fun enhancedProperty(property: FirVariableSymbol<*>, name: Name): FirVariableSymbol<*> {
        return enhancementsCache.enhancedVariables.getValue(property, this to name)
    }

    private fun FirDeclaration.computeDefaultQualifiers() =
        typeQualifierResolver.extractAndMergeDefaultQualifiers(contextQualifiers, annotations)

    @PrivateForInline
    internal fun enhance(
        original: FirVariableSymbol<*>,
        name: Name,
    ): FirVariableSymbol<*> {
        when (val firElement = original.fir) {
            is FirEnumEntry -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val predefinedInfo =
                    PredefinedFunctionEnhancementInfo(
                        TypeEnhancementInfo(0 to JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, false)),
                        emptyList()
                    )

                val newReturnTypeRef = enhanceReturnType(firElement, firElement.computeDefaultQualifiers(), predefinedInfo)

                return buildEnumEntryCopy(firElement) {
                    symbol = FirEnumEntrySymbol(firElement.symbol.callableId)
                    returnTypeRef = newReturnTypeRef
                    origin = FirDeclarationOrigin.Enhancement
                }.symbol
            }
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val newReturnTypeRef = enhanceReturnType(
                    firElement, firElement.computeDefaultQualifiers(),
                    predefinedEnhancementInfo = null
                ).let {
                    val coneTypeOrNull = it.coneTypeOrNull
                    val lowerBound = coneTypeOrNull?.lowerBoundIfFlexible()
                    if (lowerBound != null && lowerBound.isString && firElement.isStatic && firElement.hasConstantInitializer) {
                        it.withReplacedConeType(coneTypeOrNull.withNullability(ConeNullability.NOT_NULL, session.typeContext))
                    } else {
                        it
                    }
                }

                val symbol = FirFieldSymbol(original.callableId)
                buildJavaField {
                    source = firElement.source
                    moduleData = this@FirSignatureEnhancement.moduleData
                    this.symbol = symbol
                    this.name = name
                    returnTypeRef = newReturnTypeRef
                    isFromSource = original.origin.fromSource
                    isVar = firElement.isVar
                    annotationBuilder = { firElement.annotations }
                    status = firElement.status
                    if (firElement is FirJavaField) {
                        lazyInitializer = firElement.lazyInitializer
                        lazyHasConstantInitializer = firElement.lazyHasConstantInitializer
                    } else {
                        initializer = firElement.initializer
                    }

                    dispatchReceiverType = firElement.dispatchReceiverType
                    attributes = firElement.attributes.copy()
                }
                return symbol
            }
            is FirSyntheticProperty -> {
                val propertySymbol = firElement.symbol as FirJavaOverriddenSyntheticPropertySymbol
                val getterDelegate = firElement.getter.delegate
                val overridden = firElement.overridden()
                val enhancedGetterSymbol = getterDelegate.enhanceAccessorOrNull(overridden)
                val setterDelegate = firElement.setter?.delegate
                val enhancedSetterSymbol = setterDelegate?.enhanceAccessorOrNull(overridden)
                if (enhancedGetterSymbol == null && enhancedSetterSymbol == null) {
                    return original
                }
                return buildSyntheticProperty {
                    moduleData = this@FirSignatureEnhancement.moduleData
                    this.name = name
                    symbol = FirJavaOverriddenSyntheticPropertySymbol(propertySymbol.callableId, propertySymbol.getterId)
                    delegateGetter = enhancedGetterSymbol?.fir as FirSimpleFunction? ?: getterDelegate
                    delegateSetter = enhancedSetterSymbol?.fir as FirSimpleFunction? ?: setterDelegate
                    status = firElement.status
                    deprecationsProvider = getDeprecationsProviderFromAccessors(session, delegateGetter, delegateSetter)
                    dispatchReceiverType = firElement.dispatchReceiverType
                }.symbol
            }
            else -> {
                if (original is FirPropertySymbol) return original
                errorWithAttachment("Can't make enhancement for ${original::class.java}") {
                    withFirEntry("firElement", firElement)
                }
            }
        }
    }

    private fun FirSimpleFunction.enhanceAccessorOrNull(overriddenProperties: List<FirCallableDeclaration>): FirFunctionSymbol<*>? {
        if (!symbol.isEnhanceable()) return null
        return enhancedFunction(symbol, name, overriddenProperties)
    }

    @PrivateForInline
    internal fun enhance(
        original: FirFunctionSymbol<*>,
        name: Name?,
        precomputedOverridden: List<FirCallableDeclaration>?,
    ): FirFunctionSymbol<*> {
        if (!original.isEnhanceable()) {
            return original
        }

        val firMethod = original.fir
        when (firMethod) {
            is FirJavaMethod -> performBoundsResolutionForJavaMethodOrConstructorTypeParameters(
                firMethod.typeParameters, firMethod.source, firMethod::withTypeParameterBoundsResolveLock
            )
            is FirJavaConstructor -> performBoundsResolutionForJavaMethodOrConstructorTypeParameters(
                firMethod.typeParameters, firMethod.source, firMethod::withTypeParameterBoundsResolveLock
            )
            else -> {}
        }
        return enhanceMethod(
            firMethod,
            original.callableId,
            name,
            original is FirIntersectionOverrideFunctionSymbol,
            precomputedOverridden
        )
    }

    private fun FirCallableSymbol<*>.isEnhanceable(): Boolean {
        return origin is FirDeclarationOrigin.Java || isEnhanceableIntersection()
    }

    /**
     * Intersection overrides with Java and Kotlin overridden symbols need to be enhanced so that we get non-flexible types
     * in the signature.
     * This is required for @PurelyImplements to work properly.
     *
     * We only enhance intersection overrides if their dispatch receiver is equal to [owner], i.e. we don't enhance inherited
     * intersection overrides.
     *
     * See compiler/testData/codegen/box/fakeOverride/javaInheritsKotlinIntersectionOverride.kt.
     */
    private fun FirCallableSymbol<*>.isEnhanceableIntersection(): Boolean {
        return this is FirIntersectionCallableSymbol &&
                dispatchReceiverClassLookupTagOrNull() == owner.symbol.toLookupTag() &&
                unwrapFakeOverrides<FirCallableSymbol<*>>().origin is FirDeclarationOrigin.Enhancement
    }

    private fun enhanceMethod(
        firMethod: FirFunction,
        methodId: CallableId,
        name: Name?,
        isIntersectionOverride: Boolean,
        precomputedOverridden: List<FirCallableDeclaration>?,
    ): FirFunctionSymbol<*> {
        val fakeSource = firMethod.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        val predefinedEnhancementInfo =
            SignatureBuildingComponents.signature(
                owner.symbol.classId,
                firMethod.computeJvmDescriptor { it.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack, fakeSource) }
            ).let { signature ->
                PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature]?.useWarningsIfErrorModeIsNotEnabledYet()
            }

        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == firMethod.valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${firMethod.valueParameters.size} expected"
            }
        }

        val defaultQualifiers = firMethod.computeDefaultQualifiers()
        val overriddenMembers = precomputedOverridden ?: (firMethod as? FirSimpleFunction)?.overridden().orEmpty()

        // TODO(KT-66195) handle context receivers
        val hasReceiver = overriddenMembers.any { it.receiverParameter != null }

        val newReceiverTypeRef = if (firMethod is FirSimpleFunction && hasReceiver) {
            enhanceReceiverType(firMethod, overriddenMembers, defaultQualifiers)
        } else null
        val (newReturnTypeRef, deferredCalc) = if (firMethod is FirSimpleFunction) {
            enhanceReturnType(firMethod, overriddenMembers, defaultQualifiers, predefinedEnhancementInfo)
        } else {
            firMethod.returnTypeRef to null
        }

        val enhancedValueParameterTypes = mutableListOf<FirResolvedTypeRef>()

        for ((index, valueParameter) in firMethod.valueParameters.withIndex()) {
            if (hasReceiver && index == 0) continue
            enhancedValueParameterTypes += enhanceValueParameterType(
                firMethod, overriddenMembers, hasReceiver,
                defaultQualifiers, predefinedEnhancementInfo, valueParameter,
                if (hasReceiver) index - 1 else index
            )
        }

        val functionSymbol: FirFunctionSymbol<*>
        var isJavaRecordComponent = false

        val typeParameterSubstitutionMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        var typeParameterSubstitutor: ConeSubstitutor? = null
        val declarationOrigin =
            if (isIntersectionOverride) FirDeclarationOrigin.IntersectionOverride else FirDeclarationOrigin.Enhancement

        val function = when (firMethod) {
            is FirConstructor -> {
                val symbol = FirConstructorSymbol(methodId).also { functionSymbol = it }
                val builder: FirAbstractConstructorBuilder = if (firMethod.isPrimary) {
                    FirPrimaryConstructorBuilder().apply {
                        returnTypeRef = newReturnTypeRef!! // Constructors don't have overriddens, newReturnTypeRef is never null
                        val resolvedStatus = firMethod.status as? FirResolvedDeclarationStatus
                        status = if (resolvedStatus != null) {
                            FirResolvedDeclarationStatusImpl(
                                resolvedStatus.visibility,
                                Modality.FINAL,
                                resolvedStatus.effectiveVisibility
                            )
                        } else {
                            FirDeclarationStatusImpl(firMethod.visibility, Modality.FINAL)
                        }.apply {
                            isInner = firMethod.isInner
                            // Java annotation class constructors have stable names, copy flag.
                            hasStableParameterNames = firMethod.hasStableParameterNames
                        }
                        this.symbol = symbol
                        dispatchReceiverType = firMethod.dispatchReceiverType
                        attributes = firMethod.attributes.copy()
                    }
                } else {
                    FirConstructorBuilder().apply {
                        returnTypeRef = newReturnTypeRef!! // Constructors don't have overriddens, newReturnTypeRef is never null
                        status = firMethod.status
                        this.symbol = symbol
                        dispatchReceiverType = firMethod.dispatchReceiverType
                        attributes = firMethod.attributes.copy()
                    }
                }
                builder.apply {
                    source = firMethod.source
                    moduleData = this@FirSignatureEnhancement.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = declarationOrigin
                    // TODO: we should set a new origin / containing declaration to type parameters (KT-60440)
                    this.typeParameters += firMethod.typeParameters
                }
            }
            is FirSimpleFunction -> {
                isJavaRecordComponent = firMethod.isJavaRecordComponent == true
                FirSimpleFunctionBuilder().apply {
                    source = firMethod.source
                    moduleData = this@FirSignatureEnhancement.moduleData
                    origin = declarationOrigin

                    this.name = name!!
                    status = firMethod.status
                    symbol = if (isIntersectionOverride) {
                        FirIntersectionOverrideFunctionSymbol(
                            methodId, overriddenMembers.map { it.symbol },
                            containsMultipleNonSubsumed = (firMethod.symbol as? FirIntersectionCallableSymbol)?.containsMultipleNonSubsumed == true,
                        )
                    } else {
                        FirNamedFunctionSymbol(methodId)
                    }.also { functionSymbol = it }
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    typeParameters += firMethod.typeParameters.map { typeParameter ->
                        val newTypeParameter = buildTypeParameterCopy(typeParameter) {
                            origin = declarationOrigin
                            symbol = FirTypeParameterSymbol()
                            containingDeclarationSymbol = functionSymbol
                        }
                        typeParameterSubstitutionMap[typeParameter.symbol] = ConeTypeParameterTypeImpl(
                            newTypeParameter.symbol.toLookupTag(), isNullable = false
                        )
                        newTypeParameter
                    }
                    if (typeParameterSubstitutionMap.isNotEmpty()) {
                        typeParameterSubstitutor = ConeSubstitutorByMap.create(typeParameterSubstitutionMap, session)
                    }
                    returnTypeRef = if (typeParameterSubstitutor != null && newReturnTypeRef is FirResolvedTypeRef) {
                        newReturnTypeRef.withReplacedConeType(
                            typeParameterSubstitutor?.substituteOrNull(newReturnTypeRef.coneType)
                        )
                    } else {
                        newReturnTypeRef ?: FirImplicitTypeRefImplWithoutSource
                    }
                    val substitutedReceiverTypeRef = newReceiverTypeRef?.withReplacedConeType(
                        typeParameterSubstitutor?.substituteOrNull(newReceiverTypeRef.coneType)
                    )
                    receiverParameter = substitutedReceiverTypeRef?.let { receiverType ->
                        buildReceiverParameter {
                            typeRef = receiverType
                            annotations += firMethod.valueParameters.first().annotations
                            source = receiverType.source?.fakeElement(KtFakeSourceElementKind.ReceiverFromType)
                        }
                    }
                    typeParameters.forEach { typeParameter ->
                        typeParameter.replaceBounds(
                            typeParameter.bounds.map { boundTypeRef ->
                                boundTypeRef.withReplacedConeType(typeParameterSubstitutor?.substituteOrNull(boundTypeRef.coneType))
                            }
                        )
                    }

                    dispatchReceiverType = firMethod.dispatchReceiverType
                    attributes = firMethod.attributes.copy().apply {
                        if (deferredCalc != null) {
                            deferredCallableCopyReturnType = if (typeParameterSubstitutor != null) {
                                DelegatingDeferredReturnTypeWithSubstitution(deferredCalc, typeParameterSubstitutor!!)
                            } else {
                                deferredCalc
                            }
                        }
                    }
                }
            }
            else -> errorWithAttachment("Unknown Java method to enhance: ${firMethod::class.java}") {
                withFirEntry("firMethod", firMethod)
            }
        }.apply {
            val newValueParameters = firMethod.valueParameters.zip(enhancedValueParameterTypes) { valueParameter, enhancedReturnType ->
                // Java annotation default values with binary expressions like `1.0 / 0.0`
                // are not properly supported and produce error expressions, see IDEA-207252.
                // Updating the type of an error expression causes an exception.
                if (valueParameter.defaultValue !is FirErrorExpression) {
                    valueParameter.defaultValue?.replaceConeTypeOrNull(enhancedReturnType.coneType)
                }

                buildValueParameter {
                    source = valueParameter.source
                    containingFunctionSymbol = functionSymbol
                    moduleData = this@FirSignatureEnhancement.moduleData
                    origin = declarationOrigin
                    returnTypeRef = enhancedReturnType.withReplacedConeType(
                        typeParameterSubstitutor?.substituteOrNull(enhancedReturnType.coneType)
                    )
                    this.name = valueParameter.name
                    symbol = FirValueParameterSymbol(this.name)
                    defaultValue = valueParameter.defaultValue
                    isCrossinline = valueParameter.isCrossinline
                    isNoinline = valueParameter.isNoinline
                    isVararg = valueParameter.isVararg
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    annotations += valueParameter.annotations
                }
            }
            this.valueParameters += newValueParameters
            annotations += firMethod.annotations
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(session, fromJava = true)
        }.build().apply {
            if (isJavaRecordComponent) {
                this.isJavaRecordComponent = true
            }
            updateIsOperatorFlagIfNeeded(this)
        }

        return function.symbol
    }

    private fun PredefinedFunctionEnhancementInfo.useWarningsIfErrorModeIsNotEnabledYet(): PredefinedFunctionEnhancementInfo {
        val stringVersionRepresentation = errorsSinceLanguageVersion ?: return this
        val fromVersionString =
            LanguageVersion.fromVersionString(stringVersionRepresentation) ?: error("Unexpected LV: $stringVersionRepresentation")

        if (session.languageVersionSettings.languageVersion >= fromVersionString) return this

        return warningModeClone ?: error("For not null LV $errorsSinceLanguageVersion, `warningModeClone` should not be null")
    }

    private fun updateIsOperatorFlagIfNeeded(function: FirFunction) {
        if (function !is FirSimpleFunction) return
        val isOperator = OperatorFunctionChecks.isOperator(function, session, scopeSession = null).isSuccess
        if (!isOperator) return
        val newStatus = function.status.copy(isOperator = true)
        function.replaceStatus(newStatus)
    }

    fun performBoundsResolutionForClassTypeParameters(facade: FirJavaFacade, klass: FirJavaClass, source: KtSourceElement?) {
        val typeParameters = klass.typeParameters
        if (typeParameters.isEmpty()) return
        facade.withClassTypeParameterBoundsResolveLock {
            performFirstRoundOfBoundsResolution(typeParameters, source)
        }
        enhanceTypeParameterBoundsAfterFirstRound(typeParameters, source, facade::withClassTypeParameterBoundsResolveLock)
    }

    private fun performBoundsResolutionForJavaMethodOrConstructorTypeParameters(
        typeParameters: List<FirTypeParameterRef>,
        source: KtSourceElement?,
        withTypeParameterBoundsResolveLock: (() -> Unit) -> Unit,
    ) {
        if (typeParameters.isEmpty()) return
        withTypeParameterBoundsResolveLock {
            performFirstRoundOfBoundsResolution(typeParameters, source)
        }
        enhanceTypeParameterBoundsAfterFirstRound(typeParameters, source, withTypeParameterBoundsResolveLock)
    }

    /**
     * Perform first time initialization of bounds with FirResolvedTypeRef instances
     * But after that bounds are still not enhanced and more over might have not totally correct raw types bounds
     * (see the next step in the method performSecondRoundOfBoundsResolution)
     *
     * In case of A<T extends A>, or similar cases, the bound is converted to the flexible version A<*>..A<*>?,
     * while in the end it's assumed to be A<A<*>>..A<*>?
     *
     * That's necessary because at this stage it's not quite easy to come just to the final version since for that
     * we would the need upper bounds of all the type parameters that might not yet be initialized at the moment
     *
     * See the usages of FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND
     *
     * @return false if first time initialization was done previously, true otherwise
     */
    private fun performFirstRoundOfBoundsResolution(
        typeParameters: List<FirTypeParameterRef>,
        source: KtSourceElement?,
    ): Boolean {
        for (typeParameter in typeParameters) {
            if (typeParameter is FirJavaTypeParameter) {
                if (!typeParameter.performFirstRoundOfBoundsResolution(session, javaTypeParameterStack, source)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * In most cases the result of this method is just bounds from the [performFirstRoundOfBoundsResolution]
     *
     * But for the cases like A<T extends A>
     * After the first step we've got all bounds are initialized to potentially approximated version of raw types
     * And here, we compute the final version using previously initialized bounds
     *
     * So, mostly it works just as the first step, but assumes that bounds already contain FirResolvedTypeRef
     *
     * This function never publishes its result, so all [FirJavaTypeParameter] are remaining unchanged.
     *
     * @return null if second round was done previously, or matrix of enhanced bounds for all [FirJavaTypeParameter] otherwise
     */
    private fun performSecondRoundOfBoundsResolution(
        typeParameters: List<FirTypeParameterRef>,
        source: KtSourceElement?,
    ): List<MutableList<FirResolvedTypeRef>>? {
        val result = mutableListOf<MutableList<FirResolvedTypeRef>>()
        for (typeParameter in typeParameters) {
            if (typeParameter is FirJavaTypeParameter) {
                val enhancedBounds =
                    typeParameter.performSecondRoundOfBoundsResolution(session, javaTypeParameterStack, source)
                if (enhancedBounds == null) {
                    return null
                }
                result += enhancedBounds
            }
        }
        return result
    }

    /**
     * There are four rounds of bounds resolution for Java type parameters
     * 1. Plain conversion of Java types without any enhancement (with approximated raw types)
     * 2. The same conversion, but raw types are now computed precisely
     * 3. Enhancement for top-level types (no enhancement for arguments)
     * 4. Enhancement for the whole types (with arguments)
     *
     * This method requires type parameters that have already been run through the first round
     */
    private fun enhanceTypeParameterBoundsAfterFirstRound(
        typeParameters: List<FirTypeParameterRef>,
        source: KtSourceElement?,
        withTypeParameterBoundsResolveLock: (() -> Unit) -> Unit,
    ) {
        val secondRoundBounds = performSecondRoundOfBoundsResolution(typeParameters, source)
        if (secondRoundBounds == null) {
            // null means here that everything is already enhanced to the last round
            return
        }

        // Type parameters can have interdependencies between them. Assuming that there are no top-level cycles
        // (`A : B, B : A` - invalid), the cycles can still appear when type parameters use each other in argument
        // position (`A : C<B>, B : D<A>` - valid). In this case the precise enhancement of each bound depends on
        // the others' nullability, for which we need to enhance at least its head type constructor.
        typeParameters.replaceEnhancedBounds(secondRoundBounds) { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = true)
        }
        typeParameters.replaceEnhancedBounds(secondRoundBounds) { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = false)
        }

        var currentIndex = 0
        withTypeParameterBoundsResolveLock {
            // Here we publish our results
            for (typeParameter in typeParameters) {
                /**
                 * This 'if' condition, together with currentIndex++, must be synchronized
                 * with the same 'if' inside [performFirstRoundOfBoundsResolution]
                 */
                if (typeParameter is FirJavaTypeParameter) {
                    typeParameter.storeBoundsAfterAllRounds(secondRoundBounds[currentIndex])
                    currentIndex++
                }
            }
        }
    }

    private inline fun List<FirTypeParameterRef>.replaceEnhancedBounds(
        secondRoundBounds: List<MutableList<FirResolvedTypeRef>>,
        crossinline block: (FirTypeParameter, FirResolvedTypeRef) -> FirResolvedTypeRef
    ) {
        var currentIndex = 0
        for (typeParameter in this) {
            /**
             * This 'if' condition, together with currentIndex++, must be synchronized
             * with the same 'if' inside [performFirstRoundOfBoundsResolution]
             */
            if (typeParameter is FirJavaTypeParameter) {
                secondRoundBounds[currentIndex].replaceAll { block(typeParameter, it) }
                currentIndex++
            }
        }
    }

    private fun enhanceTypeParameterBound(
        typeParameter: FirTypeParameter,
        bound: FirResolvedTypeRef,
        forceOnlyHeadTypeConstructor: Boolean,
    ): FirResolvedTypeRef = EnhancementSignatureParts(
        session, typeQualifierResolver, typeParameter, isCovariant = false, forceOnlyHeadTypeConstructor,
        AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS, contextQualifiers
    ).enhance(bound, emptyList(), FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND)


    fun enhanceSuperTypes(nonEnhancedSuperTypes: List<FirTypeRef>): List<FirTypeRef> {
        val purelyImplementedSupertype = getPurelyImplementedSupertype(moduleData.session)
        val purelyImplementedSupertypeClassId = purelyImplementedSupertype?.classId
        return buildList {
            nonEnhancedSuperTypes.mapNotNullTo(this) { superType ->
                enhanceSuperType(superType).takeUnless {
                    purelyImplementedSupertypeClassId != null && it.coneType.classId == purelyImplementedSupertypeClassId
                }
            }
            purelyImplementedSupertype?.let {
                add(buildResolvedTypeRef { type = it })
            }
        }
    }

    private fun getPurelyImplementedSupertype(session: FirSession): ConeKotlinType? {
        val purelyImplementedClassIdFromAnnotation = owner.annotations
            .firstOrNull { it.unexpandedClassId?.asSingleFqName() == JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION }
            ?.let { (it.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression) }
            ?.let { it.value as? String }
            ?.takeIf { it.isNotBlank() && isValidJavaFqName(it) }
            ?.let { ClassId.topLevel(FqName(it)) }
        val purelyImplementedClassId = purelyImplementedClassIdFromAnnotation
            ?: FakePureImplementationsProvider.getPurelyImplementedInterface(owner.symbol.classId)
            ?: return null
        val superTypeSymbol = session.symbolProvider.getClassLikeSymbolByClassId(purelyImplementedClassId) ?: return null
        val superTypeParameterSymbols = superTypeSymbol.typeParameterSymbols
        val typeParameters = owner.typeParameters
        val supertypeParameterCount = superTypeParameterSymbols.size
        val typeParameterCount = typeParameters.size
        val parametersAsTypeProjections = when {
            typeParameterCount == supertypeParameterCount ->
                typeParameters.map { ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(it.symbol), isNullable = false) }
            typeParameterCount == 1 && supertypeParameterCount > 1 && purelyImplementedClassIdFromAnnotation == null -> {
                val projection = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(typeParameters.first().symbol), isNullable = false)
                (1..supertypeParameterCount).map { projection }
            }
            else -> return null
        }
        return ConeClassLikeTypeImpl(
            purelyImplementedClassId.toLookupTag(),
            parametersAsTypeProjections.toTypedArray(),
            isNullable = false
        )
    }

    private fun enhanceSuperType(type: FirTypeRef): FirTypeRef =
        EnhancementSignatureParts(
            session, typeQualifierResolver, null, isCovariant = false, forceOnlyHeadTypeConstructor = false,
            AnnotationQualifierApplicabilityType.TYPE_USE, contextQualifiers
        ).enhance(type, emptyList(), FirJavaTypeConversionMode.SUPERTYPE)

    // ================================================================================================

    private fun enhanceReceiverType(
        ownerFunction: FirSimpleFunction,
        overriddenMembers: List<FirCallableDeclaration>,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
    ): FirResolvedTypeRef {
        return ownerFunction.enhanceValueParameter(
            overriddenMembers,
            ownerFunction,
            defaultQualifiers,
            TypeInSignature.Receiver,
            predefined = null,
            forAnnotationMember = false
        )
    }

    private fun enhanceValueParameterType(
        ownerFunction: FirFunction,
        overriddenMembers: List<FirCallableDeclaration>,
        hasReceiver: Boolean,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?,
        ownerParameter: FirValueParameter,
        index: Int,
    ): FirResolvedTypeRef {
        return ownerFunction.enhanceValueParameter(
            overriddenMembers,
            ownerParameter,
            defaultQualifiers,
            TypeInSignature.ValueParameter(hasReceiver, index),
            predefinedEnhancementInfo?.parametersInfo?.getOrNull(index),
            forAnnotationMember = owner.classKind == ClassKind.ANNOTATION_CLASS
        )
    }

    private fun enhanceReturnType(
        owner: FirCallableDeclaration,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?,
    ): FirResolvedTypeRef {
        return enhanceReturnType(owner, emptyList(), defaultQualifiers, predefinedEnhancementInfo).first!!
    }

    /**
     * Either returns a not-null [FirResolvedTypeRef] or a not-null [DeferredCallableCopyReturnType], never both.
     *
     * [DeferredCallableCopyReturnType] can only (but doesn't need to) be not-null when [overriddenMembers] is non-empty.
     */
    private fun enhanceReturnType(
        owner: FirCallableDeclaration,
        overriddenMembers: List<FirCallableDeclaration>,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?,
    ): Pair<FirResolvedTypeRef?, DeferredCallableCopyReturnType?> {
        val containerApplicabilityType = if (owner is FirJavaField) {
            AnnotationQualifierApplicabilityType.FIELD
        } else {
            AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
        }
        val forAnnotationMember = if (owner is FirJavaField) {
            // Fields in annotation interfaces are constant declarations that can have any Java type.
            // For example: public @interface MyApi { Integer field = -1; }
            // Therefore, for such annotation interface fields, we use default Java type enhancement.
            false
        } else {
            this.owner.classKind == ClassKind.ANNOTATION_CLASS
        }

        // If any overridden member has implicit return type, we need to defer the return type computation.
        if (overriddenMembers.any { it.returnTypeRef is FirImplicitTypeRef }) {
            val deferredReturnTypeCalculation = object : DeferredCallableCopyReturnType() {
                override fun computeReturnType(calc: CallableCopyTypeCalculator): ConeKotlinType {
                    return owner.enhance(
                        overriddenMembers,
                        owner,
                        isCovariant = true,
                        defaultQualifiers,
                        containerApplicabilityType,
                        typeInSignature = TypeInSignature.ReturnPossiblyDeferred(calc),
                        predefinedEnhancementInfo?.returnTypeInfo,
                        forAnnotationMember = forAnnotationMember
                    ).type
                }

                override fun toString(): String = "Deferred for Enhancement (Overriddens with Implicit Types)"
            }
            return null to deferredReturnTypeCalculation
        }

        return owner.enhance(
            overriddenMembers,
            owner,
            isCovariant = true,
            defaultQualifiers,
            containerApplicabilityType,
            TypeInSignature.Return,
            predefinedEnhancementInfo?.returnTypeInfo,
            forAnnotationMember = forAnnotationMember
        ) to null
    }

    private abstract class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableDeclaration): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef = member.returnTypeRef
        }

        class ReturnPossiblyDeferred(private val calc: CallableCopyTypeCalculator) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef {
                return if (member.isJava) {
                    member.returnTypeRef
                } else {
                    calc.computeReturnType(member) ?: buildErrorTypeRef {
                        diagnostic = ConeSimpleDiagnostic("Could not resolve returnType for $member")
                    }
                }
            }
        }

        object Receiver : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef {
                if (member is FirSimpleFunction && member.isJava) {
                    return member.valueParameters[0].returnTypeRef
                }
                return member.receiverParameter?.typeRef!!
            }
        }

        class ValueParameter(val hasReceiver: Boolean, val index: Int) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef {
                // When we enhance a setter override, the overridden property's return type corresponds to the setter's value parameter.
                if (member is FirProperty) {
                    return member.returnTypeRef
                }
                if (hasReceiver && member is FirSimpleFunction && member.isJava) {
                    return member.valueParameters[index + 1].returnTypeRef
                }
                return (member as FirFunction).valueParameters[index].returnTypeRef
            }
        }
    }

    private fun FirFunction.enhanceValueParameter(
        overriddenMembers: List<FirCallableDeclaration>,
        parameterContainer: FirAnnotationContainer?,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
        typeInSignature: TypeInSignature,
        predefined: TypeEnhancementInfo?,
        forAnnotationMember: Boolean,
    ): FirResolvedTypeRef = enhance(
        overriddenMembers,
        parameterContainer ?: this,
        isCovariant = false,
        parameterContainer?.let {
            typeQualifierResolver.extractAndMergeDefaultQualifiers(defaultQualifiers, it.annotations)
        } ?: defaultQualifiers,
        VALUE_PARAMETER,
        typeInSignature,
        predefined,
        forAnnotationMember
    )

    private fun FirCallableDeclaration.enhance(
        overriddenMembers: List<FirCallableDeclaration>,
        typeContainer: FirAnnotationContainer?,
        isCovariant: Boolean,
        containerQualifiers: JavaTypeQualifiersByElementType?,
        containerApplicabilityType: AnnotationQualifierApplicabilityType,
        typeInSignature: TypeInSignature,
        predefined: TypeEnhancementInfo?,
        forAnnotationMember: Boolean,
    ): FirResolvedTypeRef {
        val typeRef = typeInSignature.getTypeRef(this)
        val typeRefsFromOverridden = overriddenMembers.map { typeInSignature.getTypeRef(it) }
        val mode = when {
            !forAnnotationMember ->
                FirJavaTypeConversionMode.DEFAULT
            containerApplicabilityType == VALUE_PARAMETER && (typeContainer as? FirValueParameter)?.name == DEFAULT_VALUE_PARAMETER ->
                FirJavaTypeConversionMode.ANNOTATION_CONSTRUCTOR_PARAMETER
            else ->
                FirJavaTypeConversionMode.ANNOTATION_MEMBER
        }
        return EnhancementSignatureParts(
            session, typeQualifierResolver, typeContainer, isCovariant, forceOnlyHeadTypeConstructor = false,
            containerApplicabilityType, containerQualifiers
        ).enhance(typeRef, typeRefsFromOverridden, mode, predefined)
    }

    private fun EnhancementSignatureParts.enhance(
        typeRef: FirTypeRef, typeRefsFromOverridden: List<FirTypeRef>,
        mode: FirJavaTypeConversionMode, predefined: TypeEnhancementInfo? = null,
    ): FirResolvedTypeRef {
        val typeWithoutEnhancement = typeRef.toConeKotlinType(mode, typeRef.source)
        val typesFromOverridden = typeRefsFromOverridden.map { it.toConeKotlinType(mode, typeRef.source) }
        val qualifiers = typeWithoutEnhancement.computeIndexedQualifiers(typesFromOverridden, predefined)
        return buildResolvedTypeRef {
            type = typeWithoutEnhancement.enhance(session, qualifiers) ?: typeWithoutEnhancement
            annotations += typeRef.annotations
            source = typeRef.source
        }
    }

    private fun FirTypeRef.toConeKotlinType(mode: FirJavaTypeConversionMode, source: KtSourceElement?): ConeKotlinType =
        toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack, source, mode)
}

/**
 * Delegates computation of return type to [deferredCalc] and substitutes the resulting type with [substitutor].
 */
private class DelegatingDeferredReturnTypeWithSubstitution(
    private val deferredCalc: DeferredCallableCopyReturnType,
    private val substitutor: ConeSubstitutor,
) : DeferredCallableCopyReturnType() {
    override fun computeReturnType(calc: CallableCopyTypeCalculator): ConeKotlinType? {
        return deferredCalc.computeReturnType(calc)?.let(substitutor::substituteOrSelf)
    }

    override fun toString(): String {
        return "DelegatingDeferredReturnTypeWithSubstitution(deferredCalc=$deferredCalc, substitutor=$substitutor)"
    }
}

private class EnhancementSignatureParts(
    private val session: FirSession,
    override val annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    private val typeContainer: FirAnnotationContainer?,
    override val isCovariant: Boolean,
    override val forceOnlyHeadTypeConstructor: Boolean,
    override val containerApplicabilityType: AnnotationQualifierApplicabilityType,
    override val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?,
) : AbstractSignatureParts<FirAnnotation>() {
    override val enableImprovementsInStrictMode: Boolean
        get() = true

    override val skipRawTypeArguments: Boolean
        get() = false

    override val containerAnnotations: Iterable<FirAnnotation>
        get() = typeContainer?.annotations ?: emptyList()

    override val containerIsVarargParameter: Boolean
        get() = typeContainer is FirValueParameter && typeContainer.isVararg

    override val typeSystem: TypeSystemContext
        get() = session.typeContext

    override fun FirAnnotation.forceWarning(unenhancedType: KotlinTypeMarker?): Boolean = this is FirJavaExternalAnnotation

    override val KotlinTypeMarker.annotations: Iterable<FirAnnotation>
        get() = (this as ConeKotlinType).customAnnotations

    override val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
        get() = (this as? ConeClassLikeType)?.lookupTag?.classId?.asSingleFqName()?.toUnsafe()

    override val KotlinTypeMarker.enhancedForWarnings: KotlinTypeMarker?
        get() = (this as ConeKotlinType).enhancedTypeForWarning

    override fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, this, other)

    override fun KotlinTypeMarker.isArrayOrPrimitiveArray(): Boolean = (this as ConeKotlinType).isArrayOrPrimitiveArray

    override val TypeParameterMarker.isFromJava: Boolean
        get() = (this as ConeTypeParameterLookupTag).symbol.fir.origin is FirDeclarationOrigin.Java

    override fun getDefaultNullability(
        referencedParameterBoundsNullability: NullabilityQualifierWithMigrationStatus?,
        defaultTypeQualifiers: JavaDefaultQualifiers?
    ): NullabilityQualifierWithMigrationStatus? {
        return referencedParameterBoundsNullability?.takeIf { it.qualifier == NullabilityQualifier.NOT_NULL }
            ?: defaultTypeQualifiers?.nullabilityQualifier
    }
}

class FirEnhancedSymbolsStorage(private val cachesFactory: FirCachesFactory) : FirSessionComponent {
    constructor(session: FirSession) : this(session.firCachesFactory)

    val cacheByOwner: FirCache<FirRegularClassSymbol, EnhancementSymbolsCache, Nothing?> =
        cachesFactory.createCache { _ -> EnhancementSymbolsCache(cachesFactory) }

    class FunctionEnhancementContext(
        val enhancement: FirSignatureEnhancement,
        val name: Name?,
        val precomputedOverridden: List<FirCallableDeclaration>?,
    )

    class EnhancementSymbolsCache(cachesFactory: FirCachesFactory) {
        @OptIn(PrivateForInline::class)
        val enhancedFunctions: FirCache<FirFunctionSymbol<*>, FirFunctionSymbol<*>, FunctionEnhancementContext> =
            cachesFactory.createCacheWithPostCompute(
                createValue = { original, context ->
                    context.enhancement.enhance(original, context.name, context.precomputedOverridden) to context.enhancement
                },
                postCompute = { _, enhancedVersion, enhancement ->
                    val enhancedVersionFir = enhancedVersion.fir
                    (enhancedVersionFir.initialSignatureAttr)?.let {
                        enhancedVersionFir.initialSignatureAttr = enhancement.enhancedFunction(it, it.name)
                    }
                }
            )

        @OptIn(PrivateForInline::class)
        val enhancedVariables: FirCache<FirVariableSymbol<*>, FirVariableSymbol<*>, Pair<FirSignatureEnhancement, Name>> =
            cachesFactory.createCache { original, (enhancement, name) ->
                enhancement.enhance(original, name)
            }
    }
}

private val FirSession.enhancedSymbolStorage: FirEnhancedSymbolsStorage by FirSession.sessionComponentAccessor()
