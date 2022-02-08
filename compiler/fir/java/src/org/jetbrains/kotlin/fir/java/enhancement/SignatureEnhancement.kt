/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirPrimaryConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

    private val contextQualifiers: JavaTypeQualifiersByElementType? =
        typeQualifierResolver.extractDefaultQualifiers(owner)

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
                return firElement.symbol.apply {
                    this.fir.replaceReturnTypeRef(newReturnTypeRef)
                    session.lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, this.fir.source, null)
                }
            }
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val newReturnTypeRef = enhanceReturnType(
                    firElement, emptyList(), firElement.computeDefaultQualifiers(),
                    predefinedEnhancementInfo = null
                ).let {
                    val lowerBound = it.type.lowerBoundIfFlexible()

                    if (firElement.isStatic && firElement.initializer != null && (lowerBound.isString || lowerBound.isInt)) {
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

                    // TODO: Use some kind of copy mechanism
                    visibility = firElement.visibility
                    modality = firElement.modality
                    isVar = firElement.isVar
                    isStatic = firElement.isStatic
                    annotationBuilder = { firElement.annotations }
                    status = firElement.status
                    initializer = firElement.initializer
                    dispatchReceiverType = firElement.dispatchReceiverType
                    attributes = firElement.attributes.copy()
                }
                return symbol
            }
            is FirSyntheticProperty -> {
                val accessorSymbol = firElement.symbol
                val getterDelegate = firElement.getter.delegate
                val enhancedGetterSymbol = if (getterDelegate is FirJavaMethod) {
                    enhanceMethod(
                        getterDelegate, getterDelegate.symbol.callableId, getterDelegate.name,
                    )
                } else {
                    getterDelegate.symbol
                }
                val setterDelegate = firElement.setter?.delegate
                val enhancedSetterSymbol = if (setterDelegate is FirJavaMethod) {
                    enhanceMethod(
                        setterDelegate, setterDelegate.symbol.callableId, setterDelegate.name,
                    )
                } else {
                    setterDelegate?.symbol
                }
                if (getterDelegate !is FirJavaMethod && setterDelegate !is FirJavaMethod) return original
                return buildSyntheticProperty {
                    moduleData = this@FirSignatureEnhancement.moduleData
                    this.name = name
                    symbol = FirJavaOverriddenSyntheticPropertySymbol(accessorSymbol.callableId, accessorSymbol.getterId)
                    delegateGetter = enhancedGetterSymbol.fir as FirSimpleFunction
                    delegateSetter = enhancedSetterSymbol?.fir as FirSimpleFunction?
                    status = firElement.status
                    deprecation = getDeprecationsFromAccessors(delegateGetter, delegateSetter, session.languageVersionSettings.apiVersion)
                }.symbol
            }
            else -> {
                if (original is FirPropertySymbol || original is FirSyntheticPropertySymbol) return original
                error("Can't make enhancement for $original: `${firElement.render()}`")
            }
        }
    }

    @PrivateForInline
    internal fun enhance(
        original: FirFunctionSymbol<*>,
        name: Name?
    ): FirFunctionSymbol<*> {
        val firMethod = original.fir

        if (firMethod !is FirJavaMethod && firMethod !is FirJavaConstructor) {
            return original
        }
        enhanceTypeParameterBounds(firMethod.typeParameters)
        return enhanceMethod(firMethod, original.callableId, name).also { enhancedVersion ->
            val enhancedVersionFir = enhancedVersion.fir
            (enhancedVersionFir.initialSignatureAttr as? FirSimpleFunction)?.let {
                enhancedVersionFir.initialSignatureAttr = enhancedFunction(it.symbol, it.name).fir
            }
        }
    }

    private fun enhanceMethod(
        firMethod: FirFunction,
        methodId: CallableId,
        name: Name?
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
        val hasReceiver = overriddenMembers.any { it.receiverTypeRef != null }

        val newReceiverTypeRef = if (firMethod is FirJavaMethod && hasReceiver) {
            enhanceReceiverType(firMethod, overriddenMembers, defaultQualifiers)
        } else null
        val newReturnTypeRef = if (firMethod is FirJavaMethod) {
            enhanceReturnType(firMethod, overriddenMembers, defaultQualifiers, predefinedEnhancementInfo)
        } else {
            firMethod.returnTypeRef
        }

        val enhancedValueParameterTypes = mutableListOf<FirResolvedTypeRef>()

        for ((index, valueParameter) in firMethod.valueParameters.withIndex()) {
            if (hasReceiver && index == 0) continue
            enhancedValueParameterTypes += enhanceValueParameterType(
                firMethod, overriddenMembers, hasReceiver,
                defaultQualifiers, predefinedEnhancementInfo, valueParameter as FirJavaValueParameter,
                if (hasReceiver) index - 1 else index
            )
        }

        val newValueParameters = firMethod.valueParameters.zip(enhancedValueParameterTypes) { valueParameter, enhancedReturnType ->
            valueParameter.defaultValue?.replaceTypeRef(enhancedReturnType)

            buildValueParameter {
                source = valueParameter.source
                moduleData = this@FirSignatureEnhancement.moduleData
                origin = FirDeclarationOrigin.Enhancement
                returnTypeRef = enhancedReturnType
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
        val function = when (firMethod) {
            is FirJavaConstructor -> {
                val symbol = FirConstructorSymbol(methodId)
                if (firMethod.isPrimary) {
                    FirPrimaryConstructorBuilder().apply {
                        returnTypeRef = newReturnTypeRef
                        val resolvedStatus = firMethod.status.safeAs<FirResolvedDeclarationStatus>()
                        status = if (resolvedStatus != null) {
                            FirResolvedDeclarationStatusImpl(
                                resolvedStatus.visibility,
                                Modality.FINAL,
                                resolvedStatus.effectiveVisibility
                            )
                        } else {
                            FirDeclarationStatusImpl(firMethod.visibility, Modality.FINAL)
                        }.apply {
                            isExpect = false
                            isActual = false
                            isOverride = false
                            isInner = firMethod.isInner
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
                    origin = FirDeclarationOrigin.Enhancement
                    this.valueParameters += newValueParameters
                    this.typeParameters += firMethod.typeParameters
                }
            }
            is FirJavaMethod -> {
                FirSimpleFunctionBuilder().apply {
                    source = firMethod.source
                    moduleData = this@FirSignatureEnhancement.moduleData
                    origin = FirDeclarationOrigin.Enhancement
                    returnTypeRef = newReturnTypeRef
                    receiverTypeRef = newReceiverTypeRef
                    this.name = name!!
                    status = firMethod.status
                    symbol = FirNamedFunctionSymbol(methodId)
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    valueParameters += newValueParameters
                    typeParameters += firMethod.typeParameters
                    dispatchReceiverType = firMethod.dispatchReceiverType
                    attributes = firMethod.attributes.copy()
                }
            }
            else -> throw AssertionError("Unknown Java method to enhance: ${firMethod.render()}")
        }.apply {
            annotations += firMethod.annotations
            deprecation = annotations.getDeprecationInfosFromAnnotations(session.languageVersionSettings.apiVersion, fromJava = true)
        }.build()

        return function.symbol
    }

    fun enhanceTypeParameterBounds(typeParameters: List<FirTypeParameterRef>) {
        // Type parameters can have interdependencies between them. Assuming that there are no top-level cycles
        // (`A : B, B : A` - invalid), the cycles can still appear when type parameters use each other in argument
        // position (`A : C<B>, B : D<A>` - valid). In this case the precise enhancement of each bound depends on
        // the others' nullability, for which we need to enhance at least its head type constructor.
        //
        // While this is straightforward to do within a single class/method (enhance all bounds' head type
        // constructors, then enhance fully), it's not so simple when two classes depend on each other (we need
        // to enhance *both* classes' type parameters' bounds' heads first). This is why we replace each bound
        // with an unenhanced version first: this ensures that the frontend at least doesn't fail.
        //
        // TODO: find a way to partially enhance type parameters of all classes before fully enhancing anything.
        // TODO: should this be done in topological order on head type constructors?
        //   I.e. for `A : B, B : C<A>` should we process `B` first?
        typeParameters.replaceBounds { _, bound ->
            bound.resolveIfJavaType(session, javaTypeParameterStack, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND)
        }
        typeParameters.replaceBounds { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = true)
        }
        typeParameters.replaceBounds { typeParameter, bound ->
            enhanceTypeParameterBound(typeParameter, bound, forceOnlyHeadTypeConstructor = false)
        }
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
        ).enhance(bound, emptyList(), FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND)

    fun enhanceSuperType(type: FirTypeRef): FirTypeRef =
        EnhancementSignatureParts(
            session, typeQualifierResolver, null, isCovariant = false, forceOnlyHeadTypeConstructor = false,
            AnnotationQualifierApplicabilityType.TYPE_USE, contextQualifiers
        ).enhance(type, emptyList(), FirJavaTypeConversionMode.SUPERTYPE)

    // ================================================================================================

    private fun enhanceReceiverType(
        ownerFunction: FirJavaMethod,
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
        ownerParameter: FirJavaValueParameter,
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
        val containerApplicabilityType = if (owner is FirJavaField)
            AnnotationQualifierApplicabilityType.FIELD
        else
            AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
        return owner.enhance(
            overriddenMembers,
            owner,
            isCovariant = true,
            defaultQualifiers,
            containerApplicabilityType,
            TypeInSignature.Return,
            predefinedEnhancementInfo?.returnTypeInfo,
            forAnnotationMember = this.owner.classKind == ClassKind.ANNOTATION_CLASS
        )
    }

    private sealed class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableDeclaration): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef = member.returnTypeRef
        }

        object Receiver : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef {
                if (member is FirJavaMethod) return member.valueParameters[0].returnTypeRef
                return member.receiverTypeRef!!
            }
        }

        class ValueParameter(val hasReceiver: Boolean, val index: Int) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableDeclaration): FirTypeRef {
                if (hasReceiver && member is FirJavaMethod) {
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
        AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
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
        val mode = if (forAnnotationMember) FirJavaTypeConversionMode.ANNOTATION_MEMBER else FirJavaTypeConversionMode.DEFAULT
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

    override val FirAnnotation.forceWarning: Boolean
        get() = false // TODO: force warnings on IDEA external annotations

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
        get() = (this as ConeTypeParameterLookupTag).symbol.fir.origin == FirDeclarationOrigin.Java
}

class FirEnhancedSymbolsStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val cacheByOwner: FirCache<FirRegularClassSymbol, EnhancementSymbolsCache, Nothing?> =
        cachesFactory.createCache { _ -> EnhancementSymbolsCache(cachesFactory) }

    class EnhancementSymbolsCache(cachesFactory: FirCachesFactory) {
        @OptIn(PrivateForInline::class)
        val enhancedFunctions: FirCache<FirFunctionSymbol<*>, FirFunctionSymbol<*>, Pair<FirSignatureEnhancement, Name?>> =
            cachesFactory.createCache { original, (enhancement, name) ->
                enhancement.enhance(original, name)
            }

        @OptIn(PrivateForInline::class)
        val enhancedVariables: FirCache<FirVariableSymbol<*>, FirVariableSymbol<*>, Pair<FirSignatureEnhancement, Name>> =
            cachesFactory.createCache { original, (enhancement, name) ->
                enhancement.enhance(original, name)
            }
    }
}

private val FirSession.enhancedSymbolStorage: FirEnhancedSymbolsStorage by FirSession.sessionComponentAccessor()
