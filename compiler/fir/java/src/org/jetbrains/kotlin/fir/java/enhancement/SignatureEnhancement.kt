/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.unexpandedClassId
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType.VALUE_PARAMETER
import org.jetbrains.kotlin.load.java.FakePureImplementationsProvider
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
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
    private val overridden: FirSimpleFunction.() -> List<FirCallableDeclaration>
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

    fun enhancedFunction(function: FirFunctionSymbol<*>, name: Name?): FirFunctionSymbol<*> {
        return enhancementsCache.enhancedFunctions.getValue(function, this to name)
    }

    fun enhancedProperty(property: FirVariableSymbol<*>, name: Name): FirVariableSymbol<*> {
        return enhancementsCache.enhancedVariables.getValue(property, this to name)
    }

    private fun FirDeclaration.computeDefaultQualifiers() =
        typeQualifierResolver.extractAndMergeDefaultQualifiers(contextQualifiers, annotations)

    @PrivateForInline
    internal fun enhance(
        original: FirVariableSymbol<*>,
        name: Name
    ): FirVariableSymbol<*> {
        when (val firElement = original.fir) {
            is FirEnumEntry -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val predefinedInfo =
                    PredefinedFunctionEnhancementInfo(
                        TypeEnhancementInfo(0 to JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, false)),
                        emptyList()
                    )

                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), firElement.computeDefaultQualifiers(), predefinedInfo)

                return buildEnumEntryCopy(firElement) {
                    symbol = FirEnumEntrySymbol(firElement.symbol.callableId)
                    returnTypeRef = newReturnTypeRef
                    origin = FirDeclarationOrigin.Enhancement
                }.apply {
                    session.lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, this.source, null)
                }.symbol
            }
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val newReturnTypeRef = enhanceReturnType(
                    firElement, emptyList(), firElement.computeDefaultQualifiers(),
                    predefinedEnhancementInfo = null
                ).let {
                    val lowerBound = it.type.lowerBoundIfFlexible()
                    if (lowerBound.isString && firElement.isStatic && firElement.hasConstantInitializer) {
                        it.withReplacedConeType(it.type.withNullability(ConeNullability.NOT_NULL, session.typeContext))
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

                    // TODO: Use some kind of copy mechanism
                    visibility = firElement.visibility
                    modality = firElement.modality
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
                val accessorSymbol = firElement.symbol
                val getterDelegate = firElement.getter.delegate
                val enhancedGetterSymbol = getterDelegate.enhanceAccessorOrNull()
                val setterDelegate = firElement.setter?.delegate
                val enhancedSetterSymbol = setterDelegate?.enhanceAccessorOrNull()
                if (enhancedGetterSymbol == null && enhancedSetterSymbol == null) {
                    return original
                }
                return buildSyntheticProperty {
                    moduleData = this@FirSignatureEnhancement.moduleData
                    this.name = name
                    symbol = FirJavaOverriddenSyntheticPropertySymbol(accessorSymbol.callableId, accessorSymbol.getterId)
                    delegateGetter = enhancedGetterSymbol?.fir as FirSimpleFunction? ?: getterDelegate
                    delegateSetter = enhancedSetterSymbol?.fir as FirSimpleFunction? ?: setterDelegate
                    status = firElement.status
                    deprecationsProvider = getDeprecationsProviderFromAccessors(session, delegateGetter, delegateSetter)
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

    private fun FirSimpleFunction.enhanceAccessorOrNull(): FirFunctionSymbol<*>? {
        if (!symbol.isEnhanceable()) return null
        return enhancedFunction(symbol, name)
    }

    @PrivateForInline
    internal fun enhance(
        original: FirFunctionSymbol<*>,
        name: Name?,
    ): FirFunctionSymbol<*> {
        if (!original.isEnhanceable()) {
            return original
        }

        val firMethod = original.fir
        val enhancedParameters = enhanceTypeParameterBoundsForMethod(firMethod)
        return enhanceMethod(firMethod, original.callableId, name, enhancedParameters, original is FirIntersectionOverrideFunctionSymbol)
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

    /**
     * @param enhancedTypeParameters pass enhanced type parameters that will be used instead of original ones.
     * **null** means that [enhanceMethod] will use the original type parameters to create an enhanced function
     */
    private fun enhanceMethod(
        firMethod: FirFunction,
        methodId: CallableId,
        name: Name?,
        enhancedTypeParameters: List<FirTypeParameterRef>?,
        isIntersectionOverride: Boolean,
    ): FirFunctionSymbol<*> {
        val predefinedEnhancementInfo =
            SignatureBuildingComponents.signature(
                owner.symbol.classId,
                firMethod.computeJvmDescriptor { it.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack) }
            ).let { signature ->
                PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature]
            }

        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == firMethod.valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${firMethod.valueParameters.size} expected"
            }
        }

        val defaultQualifiers = firMethod.computeDefaultQualifiers()
        val overriddenMembers = (firMethod as? FirSimpleFunction)?.overridden().orEmpty()
        val hasReceiver = overriddenMembers.any { it.receiverParameter != null }

        val newReceiverTypeRef = if (firMethod is FirSimpleFunction && hasReceiver) {
            enhanceReceiverType(firMethod, overriddenMembers, defaultQualifiers)
        } else null
        val newReturnTypeRef = if (firMethod is FirSimpleFunction) {
            enhanceReturnType(firMethod, overriddenMembers, defaultQualifiers, predefinedEnhancementInfo)
        } else {
            firMethod.returnTypeRef
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
        var typeParameterSubstitutor: ConeSubstitutorByMap? = null
        val declarationOrigin =
            if (isIntersectionOverride) FirDeclarationOrigin.IntersectionOverride else FirDeclarationOrigin.Enhancement

        val function = when (firMethod) {
            is FirConstructor -> {
                val symbol = FirConstructorSymbol(methodId).also { functionSymbol = it }
                if (firMethod.isPrimary) {
                    FirPrimaryConstructorBuilder().apply {
                        returnTypeRef = newReturnTypeRef
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
                        returnTypeRef = newReturnTypeRef
                        status = firMethod.status
                        this.symbol = symbol
                        dispatchReceiverType = firMethod.dispatchReceiverType
                        attributes = firMethod.attributes.copy()
                    }
                }.apply {
                    source = firMethod.source
                    moduleData = this@FirSignatureEnhancement.moduleData
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = declarationOrigin
                    // TODO: we should set a new origin / containing declaration to type parameters (KT-60440)
                    this.typeParameters += (enhancedTypeParameters ?: firMethod.typeParameters)
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
                        FirIntersectionOverrideFunctionSymbol(methodId, overriddenMembers.map { it.symbol })
                    } else {
                        FirNamedFunctionSymbol(methodId)
                    }.also { functionSymbol = it }
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    typeParameters += (enhancedTypeParameters ?: firMethod.typeParameters).map { typeParameter ->
                        // FirJavaMethod contains only FirTypeParameter so [enhancedTypeParameters] must have the same type
                        require(typeParameter is FirTypeParameter) {
                            "Unexpected type parameter type: ${typeParameter::class.simpleName}"
                        }

                        // TODO: we probably shouldn't build a copy second time. See performFirstRoundOfBoundsResolution (KT-60446)
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
                        typeParameterSubstitutor = ConeSubstitutorByMap(typeParameterSubstitutionMap, session)
                    }
                    returnTypeRef = newReturnTypeRef.withReplacedConeType(
                        typeParameterSubstitutor?.substituteOrNull(newReturnTypeRef.coneType)
                    )
                    val substitutedReceiverTypeRef = newReceiverTypeRef?.withReplacedConeType(
                        typeParameterSubstitutor?.substituteOrNull(newReturnTypeRef.coneType)
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
                    attributes = firMethod.attributes.copy()
                }
            }
            else -> errorWithAttachment("Unknown Java method to enhance: ${firMethod::class.java}") {
                withFirEntry("firMethod", firMethod)
            }
        }.apply {
            val newValueParameters = firMethod.valueParameters.zip(enhancedValueParameterTypes) { valueParameter, enhancedReturnType ->
                valueParameter.defaultValue?.replaceConeTypeOrNull(enhancedReturnType.coneType)

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

    private fun updateIsOperatorFlagIfNeeded(function: FirFunction) {
        if (function !is FirSimpleFunction) return
        val isOperator = OperatorFunctionChecks.isOperator(function, session, scopeSession = null).isSuccess
        if (!isOperator) return
        val newStatus = function.status.copy(isOperator = true)
        function.replaceStatus(newStatus)
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
     */
    fun performFirstRoundOfBoundsResolution(typeParameters: List<FirTypeParameterRef>): Pair<List<List<FirTypeRef>>, List<FirTypeParameterRef>> {
        val initialBounds: MutableList<List<FirTypeRef>> = mutableListOf()
        val typeParametersCopy = ArrayList<FirTypeParameterRef>(typeParameters.size)
        for (typeParameter in typeParameters) {
            typeParametersCopy += if (typeParameter is FirTypeParameter) {
                initialBounds.add(typeParameter.bounds.toList())
                buildTypeParameterCopy(typeParameter) {
                    // TODO: we should create a new symbol to avoid clashing (KT-60445)
                    bounds.clear()
                    typeParameter.bounds.mapTo(bounds) {
                        it.resolveIfJavaType(session, javaTypeParameterStack, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND)
                    }
                }
            } else {
                typeParameter
            }
        }

        return initialBounds to typeParametersCopy
    }

    /**
     * In most cases that method doesn't change anything
     *
     * But the cases like A<T extends A>
     * After the first step we've got all bounds are initialized to potentially approximated version of raw types
     * And here, we compute the final version using previously initialized bounds
     *
     * So, mostly it works just as the first step, but assumes that bounds already contain FirResolvedTypeRef
     */
    private fun performSecondRoundOfBoundsResolution(
        typeParameters: List<FirTypeParameterRef>,
        initialBounds: List<List<FirTypeRef>>
    ) {
        var currentIndex = 0
        for (typeParameter in typeParameters) {
            if (typeParameter is FirTypeParameter) {
                typeParameter.replaceBounds(initialBounds[currentIndex].map {
                    it.resolveIfJavaType(session, javaTypeParameterStack, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND)
                })

                currentIndex++
            }
        }
    }

    /**
     * There are four rounds of bounds resolution for Java type parameters
     * 1. Plain conversion of Java types without any enhancement (with approximated raw types)
     * 2. The same conversion, but raw types are not computed precisely
     * 3. Enhancement for top-level types (no enhancement for arguments)
     * 4. Enhancement for the whole types (with arguments)
     *
     * This method requires type parameters that have already been run through the first round
     */
    fun enhanceTypeParameterBoundsAfterFirstRound(
        typeParameters: List<FirTypeParameterRef>,
        // The state of bounds before the first round
        initialBounds: List<List<FirTypeRef>>,
    ) {
        performSecondRoundOfBoundsResolution(typeParameters, initialBounds)

        // Type parameters can have interdependencies between them. Assuming that there are no top-level cycles
        // (`A : B, B : A` - invalid), the cycles can still appear when type parameters use each other in argument
        // position (`A : C<B>, B : D<A>` - valid). In this case the precise enhancement of each bound depends on
        // the others' nullability, for which we need to enhance at least its head type constructor.
        typeParameters.replaceBounds { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = true)
        }
        typeParameters.replaceBounds { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = false)
        }
    }

    private fun enhanceTypeParameterBoundsForMethod(firMethod: FirFunction): List<FirTypeParameterRef> {
        val (initialBounds, copiedParameters) = performFirstRoundOfBoundsResolution(firMethod.typeParameters)
        enhanceTypeParameterBoundsAfterFirstRound(copiedParameters, initialBounds)
        return copiedParameters
    }

    private inline fun List<FirTypeParameterRef>.replaceBounds(block: (FirTypeParameter, FirTypeRef) -> FirTypeRef) {
        for (typeParameter in this) {
            if (typeParameter is FirTypeParameter) {
                typeParameter.replaceBounds(typeParameter.bounds.map { block(typeParameter, it) })
            }
        }
    }

    private fun enhanceTypeParameterBound(typeParameter: FirTypeParameter, bound: FirTypeRef, forceOnlyHeadTypeConstructor: Boolean) =
        EnhancementSignatureParts(
            session, typeQualifierResolver, typeParameter, isCovariant = false, forceOnlyHeadTypeConstructor,
            AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS, contextQualifiers
        ).enhance(bound, emptyList(), FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND)


    fun enhanceSuperTypes(unenhnancedSuperTypes: List<FirTypeRef>): List<FirTypeRef> {
        val purelyImplementedSupertype = getPurelyImplementedSupertype(moduleData.session)
        val purelyImplementedSupertypeClassId = purelyImplementedSupertype?.classId
        return buildList {
            unenhnancedSuperTypes.mapNotNullTo(this) { superType ->
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
            ?.let { (it.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression<*>) }
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
        defaultQualifiers: JavaTypeQualifiersByElementType?
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
        index: Int
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
        overriddenMembers: List<FirCallableDeclaration>,
        defaultQualifiers: JavaTypeQualifiersByElementType?,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
    ): FirResolvedTypeRef {
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
        return owner.enhance(
            overriddenMembers,
            owner,
            isCovariant = true,
            defaultQualifiers,
            containerApplicabilityType,
            TypeInSignature.Return,
            predefinedEnhancementInfo?.returnTypeInfo,
            forAnnotationMember = forAnnotationMember
        )
    }

    private sealed class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableDeclaration): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef = member.returnTypeRef
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
        forAnnotationMember: Boolean
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
        forAnnotationMember: Boolean
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
        mode: FirJavaTypeConversionMode, predefined: TypeEnhancementInfo? = null
    ): FirResolvedTypeRef {
        val typeWithoutEnhancement = typeRef.toConeKotlinType(mode)
        val typesFromOverridden = typeRefsFromOverridden.map { it.toConeKotlinType(mode) }
        val qualifiers = typeWithoutEnhancement.computeIndexedQualifiers(typesFromOverridden, predefined)
        return buildResolvedTypeRef {
            type = typeWithoutEnhancement.enhance(session, qualifiers) ?: typeWithoutEnhancement
            annotations += typeRef.annotations
            source = typeRef.source
        }
    }

    private fun FirTypeRef.toConeKotlinType(mode: FirJavaTypeConversionMode): ConeKotlinType =
        toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack, mode)
}

private class EnhancementSignatureParts(
    private val session: FirSession,
    override val annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    private val typeContainer: FirAnnotationContainer?,
    override val isCovariant: Boolean,
    override val forceOnlyHeadTypeConstructor: Boolean,
    override val containerApplicabilityType: AnnotationQualifierApplicabilityType,
    override val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?
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
        get() = (this as ConeKotlinType).attributes.customAnnotations

    override val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
        get() = ((this as? ConeLookupTagBasedType)?.lookupTag as? ConeClassLikeLookupTag)?.classId?.asSingleFqName()?.toUnsafe()

    override val KotlinTypeMarker.enhancedForWarnings: KotlinTypeMarker?
        get() = null // TODO: implement enhancement for warnings

    override fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, this, other)

    override fun KotlinTypeMarker.isArrayOrPrimitiveArray(): Boolean = (this as ConeKotlinType).isArrayOrPrimitiveArray

    override val TypeParameterMarker.isFromJava: Boolean
        get() = (this as ConeTypeParameterLookupTag).symbol.fir.origin is FirDeclarationOrigin.Java
}

class FirEnhancedSymbolsStorage(private val cachesFactory: FirCachesFactory) : FirSessionComponent {
    constructor(session: FirSession) : this(session.firCachesFactory)

    val cacheByOwner: FirCache<FirRegularClassSymbol, EnhancementSymbolsCache, Nothing?> =
        cachesFactory.createCache { _ -> EnhancementSymbolsCache(cachesFactory) }

    class EnhancementSymbolsCache(cachesFactory: FirCachesFactory) {
        @OptIn(PrivateForInline::class)
        val enhancedFunctions: FirCache<FirFunctionSymbol<*>, FirFunctionSymbol<*>, Pair<FirSignatureEnhancement, Name?>> =
            cachesFactory.createCacheWithPostCompute(
                createValue = { original, (enhancement, name) ->
                    enhancement.enhance(original, name) to enhancement
                },
                postCompute = { _, enhancedVersion, enhancement ->
                    val enhancedVersionFir = enhancedVersion.fir
                    (enhancedVersionFir.initialSignatureAttr as? FirSimpleFunction)?.let {
                        enhancedVersionFir.initialSignatureAttr = enhancement.enhancedFunction(it.symbol, it.name).fir
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
