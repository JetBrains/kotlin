/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
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
    private val moduleData = owner.moduleData

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (owner is FirJavaClass) owner.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private val jsr305State: JavaTypeEnhancementState = session.javaTypeEnhancementState

    private val typeQualifierResolver = FirAnnotationTypeQualifierResolver(session, jsr305State)

    private val context: FirJavaEnhancementContext =
        FirJavaEnhancementContext(session) { null }.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, owner.annotations)

    private val enhancements = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>>()

    fun enhancedFunction(
        function: FirFunctionSymbol<*>,
        name: Name?
    ): FirFunctionSymbol<*> {
        return enhancements.getOrPut(function) {
            enhance(function, name).also { enhancedVersion ->
                (enhancedVersion.fir.initialSignatureAttr as? FirSimpleFunction)?.let {
                    enhancedVersion.fir.initialSignatureAttr = enhancedFunction(it.symbol, it.name).fir
                }
            }
        } as FirFunctionSymbol<*>
    }

    fun enhancedProperty(property: FirVariableSymbol<*>, name: Name): FirVariableSymbol<*> {
        return enhancements.getOrPut(property) { enhance(property, name) } as FirVariableSymbol<*>
    }

    private fun enhance(
        original: FirVariableSymbol<*>,
        name: Name
    ): FirVariableSymbol<*> {
        when (val firElement = original.fir) {
            is FirEnumEntry -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, firElement.annotations)
                val predefinedInfo =
                    PredefinedFunctionEnhancementInfo(
                        TypeEnhancementInfo(0 to JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, false)),
                        emptyList()
                    )

                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, predefinedInfo)
                return firElement.symbol.apply {
                    this.fir.replaceReturnTypeRef(newReturnTypeRef)
                    session.lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, this.fir.source, null)
                }
            }
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, firElement.annotations)
                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, null)

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
                return buildSyntheticProperty {
                    moduleData = this@FirSignatureEnhancement.moduleData
                    this.name = name
                    symbol = FirAccessorSymbol(accessorSymbol.callableId, accessorSymbol.accessorId)
                    delegateGetter = enhancedGetterSymbol.fir as FirSimpleFunction
                    delegateSetter = enhancedSetterSymbol?.fir as FirSimpleFunction?
                    status = firElement.status
                    deprecation = getDeprecationsFromAccessors(delegateGetter, delegateSetter, session.languageVersionSettings.apiVersion)
                }.symbol
            }
            else -> {
                if (original is FirPropertySymbol || original is FirAccessorSymbol) return original
                error("Can't make enhancement for $original: `${firElement.render()}`")
            }
        }
    }

    private fun enhance(
        original: FirFunctionSymbol<*>,
        name: Name?
    ): FirFunctionSymbol<*> {
        val firMethod = original.fir

        if (firMethod !is FirJavaMethod && firMethod !is FirJavaConstructor) {
            return original
        }
        return enhanceMethod(firMethod, original.callableId, name)
    }
    
    private fun enhanceMethod(
        firMethod: FirFunction,
        methodId: CallableId,
        name: Name?
    ): FirFunctionSymbol<*> {
        val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, firMethod.annotations)

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

        val overriddenMembers = (firMethod as? FirSimpleFunction)?.overridden().orEmpty()
        val hasReceiver = overriddenMembers.any { it.receiverTypeRef != null }

        val newReceiverTypeRef = if (firMethod is FirJavaMethod && hasReceiver) {
            enhanceReceiverType(firMethod, overriddenMembers, memberContext)
        } else null
        val newReturnTypeRef = if (firMethod !is FirJavaMethod) {
            firMethod.returnTypeRef
        } else {
            enhanceReturnType(firMethod, overriddenMembers, memberContext, predefinedEnhancementInfo)
        }

        val enhancedValueParameterTypes = mutableListOf<FirResolvedTypeRef>()

        for ((index, valueParameter) in firMethod.valueParameters.withIndex()) {
            if (hasReceiver && index == 0) continue
            enhancedValueParameterTypes += enhanceValueParameterType(
                firMethod, overriddenMembers, hasReceiver,
                memberContext, predefinedEnhancementInfo, valueParameter as FirJavaValueParameter,
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


    // ================================================================================================

    private fun enhanceReceiverType(
        ownerFunction: FirJavaMethod,
        overriddenMembers: List<FirCallableDeclaration>,
        memberContext: FirJavaEnhancementContext
    ): FirResolvedTypeRef {
        return ownerFunction.partsForValueParameter(
            typeQualifierResolver,
            overriddenMembers,
            // TODO: check me
            parameterContainer = ownerFunction,
            methodContext = memberContext,
            typeInSignature = TypeInSignature.Receiver
        ).enhance(session)
    }

    private fun enhanceValueParameterType(
        ownerFunction: FirFunction,
        overriddenMembers: List<FirCallableDeclaration>,
        hasReceiver: Boolean,
        memberContext: FirJavaEnhancementContext,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?,
        ownerParameter: FirJavaValueParameter,
        index: Int
    ): FirResolvedTypeRef {
        if (ownerParameter.returnTypeRef is FirResolvedTypeRef) {
            return ownerParameter.returnTypeRef as FirResolvedTypeRef
        }
        return ownerFunction.partsForValueParameter(
            typeQualifierResolver,
            overriddenMembers,
            parameterContainer = ownerParameter,
            methodContext = memberContext,
            typeInSignature = TypeInSignature.ValueParameter(hasReceiver, index)
        ).enhance(
            session,
            predefinedEnhancementInfo?.parametersInfo?.getOrNull(index),
            forAnnotationMember = owner.classKind == ClassKind.ANNOTATION_CLASS
        )
    }

    private fun enhanceReturnType(
        owner: FirCallableDeclaration,
        overriddenMembers: List<FirCallableDeclaration>,
        memberContext: FirJavaEnhancementContext,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
    ): FirResolvedTypeRef {
        return owner.parts(
            typeQualifierResolver,
            overriddenMembers,
            typeContainer = owner, isCovariant = true,
            containerContext = memberContext,
            containerApplicabilityType =
            if (owner is FirJavaField) AnnotationQualifierApplicabilityType.FIELD
            else AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE,
            typeInSignature = TypeInSignature.Return
        ).enhance(
            session, predefinedEnhancementInfo?.returnTypeInfo, forAnnotationMember = this.owner.classKind == ClassKind.ANNOTATION_CLASS
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

    private fun FirFunction.partsForValueParameter(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableDeclaration>,
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterContainer: FirAnnotationContainer?,
        methodContext: FirJavaEnhancementContext,
        typeInSignature: TypeInSignature
    ): EnhancementSignatureParts = (this as FirCallableDeclaration).parts(
        typeQualifierResolver,
        overriddenMembers,
        parameterContainer, false,
        parameterContainer?.let {
            methodContext.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, it.annotations)
        } ?: methodContext,
        AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
        typeInSignature
    )

    private fun FirCallableDeclaration.parts(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableDeclaration>,
        typeContainer: FirAnnotationContainer?,
        isCovariant: Boolean,
        containerContext: FirJavaEnhancementContext,
        containerApplicabilityType: AnnotationQualifierApplicabilityType,
        typeInSignature: TypeInSignature
    ): EnhancementSignatureParts {
        val typeRef = typeInSignature.getTypeRef(this)
        return EnhancementSignatureParts(
            typeQualifierResolver,
            typeContainer,
            javaTypeParameterStack,
            typeRef as FirJavaTypeRef,
            overriddenMembers.map {
                typeInSignature.getTypeRef(it)
            },
            isCovariant,
            // recompute default type qualifiers using type annotations
            containerContext.copyWithNewDefaultTypeQualifiers(
                typeQualifierResolver, typeRef.annotations
            ),
            containerApplicabilityType
        )
    }
}
