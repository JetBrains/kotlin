/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirPrimaryConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSignatureEnhancement(
    private val owner: FirRegularClass,
    private val session: FirSession,
    private val overridden: FirSimpleFunction.() -> List<FirCallableMemberDeclaration<*>>
) {
    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (owner is FirJavaClass) owner.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private val jsr305State: Jsr305State = session.jsr305State ?: Jsr305State.DEFAULT

    private val typeQualifierResolver = FirAnnotationTypeQualifierResolver(session, jsr305State)

    private val context: FirJavaEnhancementContext =
        FirJavaEnhancementContext(session) { null }.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, owner.annotations)

    private val enhancements = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>>()

    fun enhancedFunction(
        function: FirFunctionSymbol<*>,
        name: Name?
    ): FirFunctionSymbol<*> {
        return enhancements.getOrPut(function) { enhance(function, name) } as FirFunctionSymbol<*>
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
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firElement.annotations)
                val predefinedInfo =
                    PredefinedFunctionEnhancementInfo(
                        TypeEnhancementInfo(0 to JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, false)),
                        emptyList()
                    )

                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, predefinedInfo)
                return firElement.symbol.apply { this.fir.replaceReturnTypeRef(newReturnTypeRef) }
            }
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firElement.annotations)
                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, null)

                val symbol = FirFieldSymbol(original.callableId)
                buildJavaField {
                    source = firElement.source
                    session = this@FirSignatureEnhancement.session
                    this.symbol = symbol
                    this.name = name
                    visibility = firElement.visibility
                    modality = firElement.modality
                    returnTypeRef = newReturnTypeRef
                    isVar = firElement.isVar
                    isStatic = firElement.isStatic
                    annotations += firElement.annotations
                    status = firElement.status
                }
                return symbol
            }
            is FirSyntheticProperty -> {
                val accessorSymbol = firElement.symbol
                val enhancedFunctionSymbol = enhanceMethod(
                    firElement.getter.delegate, accessorSymbol.accessorId, accessorSymbol.accessorId.callableName
                )
                return buildSyntheticProperty {
                    session = this@FirSignatureEnhancement.session
                    this.name = name
                    symbol = FirAccessorSymbol(accessorSymbol.callableId, accessorSymbol.accessorId)
                    delegateGetter = enhancedFunctionSymbol.fir as FirSimpleFunction
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
        firMethod: FirFunction<*>,
        methodId: CallableId,
        name: Name?
    ): FirFunctionSymbol<*> {
        val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firMethod.annotations)

        val predefinedEnhancementInfo =
            SignatureBuildingComponents.signature(owner.symbol.classId, firMethod.computeJvmDescriptor()).let { signature ->
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

        val newValueParameterInfo = mutableListOf<EnhanceValueParameterResult>()

        for ((index, valueParameter) in firMethod.valueParameters.withIndex()) {
            if (hasReceiver && index == 0) continue
            newValueParameterInfo += enhanceValueParameter(
                firMethod, overriddenMembers, hasReceiver,
                memberContext, predefinedEnhancementInfo, valueParameter as FirJavaValueParameter,
                if (hasReceiver) index - 1 else index
            )
        }

        val newValueParameters = firMethod.valueParameters.zip(newValueParameterInfo) { valueParameter, newInfo ->
            val (newTypeRef, newDefaultValue) = newInfo
            buildValueParameter {
                source = valueParameter.source
                session = this@FirSignatureEnhancement.session
                origin = FirDeclarationOrigin.Enhancement
                returnTypeRef = newTypeRef
                this.name = valueParameter.name
                symbol = FirVariableSymbol(this.name)
                defaultValue = valueParameter.defaultValue ?: newDefaultValue
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
                                firMethod.visibility,
                                Modality.FINAL
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
                    }
                } else {
                    FirConstructorBuilder().apply {
                        returnTypeRef = newReturnTypeRef
                        status = firMethod.status
                        this.symbol = symbol
                    }
                }.apply {
                    source = firMethod.source
                    session = this@FirSignatureEnhancement.session
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    origin = FirDeclarationOrigin.Enhancement
                    this.valueParameters += newValueParameters
                    this.typeParameters += firMethod.typeParameters
                }
            }
            is FirJavaMethod -> {
                FirSimpleFunctionBuilder().apply {
                    source = firMethod.source
                    session = this@FirSignatureEnhancement.session
                    origin = FirDeclarationOrigin.Enhancement
                    returnTypeRef = newReturnTypeRef
                    receiverTypeRef = newReceiverTypeRef
                    this.name = name!!
                    status = firMethod.status
                    symbol = FirNamedFunctionSymbol(methodId)
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    valueParameters += newValueParameters
                    typeParameters += firMethod.typeParameters
                }
            }
            else -> throw AssertionError("Unknown Java method to enhance: ${firMethod.render()}")
        }.apply {
            annotations += firMethod.annotations
        }.build()

        return function.symbol
    }


    // ================================================================================================

    private fun enhanceReceiverType(
        ownerFunction: FirJavaMethod,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        memberContext: FirJavaEnhancementContext
    ): FirResolvedTypeRef {
        val signatureParts = ownerFunction.partsForValueParameter(
            typeQualifierResolver,
            overriddenMembers,
            // TODO: check me
            parameterContainer = ownerFunction,
            methodContext = memberContext,
            typeInSignature = TypeInSignature.Receiver
        ).enhance(session, jsr305State)
        return signatureParts.type
    }

    private data class EnhanceValueParameterResult(val typeRef: FirResolvedTypeRef, val defaultValue: FirExpression?)

    private fun enhanceValueParameter(
        ownerFunction: FirFunction<*>,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        hasReceiver: Boolean,
        memberContext: FirJavaEnhancementContext,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?,
        ownerParameter: FirJavaValueParameter,
        index: Int
    ): EnhanceValueParameterResult {
        val signatureParts = ownerFunction.partsForValueParameter(
            typeQualifierResolver,
            overriddenMembers,
            parameterContainer = ownerParameter,
            methodContext = memberContext,
            typeInSignature = TypeInSignature.ValueParameter(hasReceiver, index)
        ).enhance(
            session,
            jsr305State,
            predefinedEnhancementInfo?.parametersInfo?.getOrNull(index),
            forAnnotationValueParameter = owner.classKind == ClassKind.ANNOTATION_CLASS
        )
        val firResolvedTypeRef = signatureParts.type
        val defaultValueExpression = when (val defaultValue = ownerParameter.getDefaultValueFromAnnotation()) {
            NullDefaultValue -> buildConstExpression(null, FirConstKind.Null, null)
            is StringDefaultValue -> firResolvedTypeRef.type.lexicalCastFrom(session, defaultValue.value)
            null -> null
        }
        return EnhanceValueParameterResult(firResolvedTypeRef, defaultValueExpression)
    }

    private fun enhanceReturnType(
        owner: FirCallableMemberDeclaration<*>,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        memberContext: FirJavaEnhancementContext,
        predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
    ): FirResolvedTypeRef {
        val signatureParts = owner.parts(
            typeQualifierResolver,
            overriddenMembers,
            typeContainer = owner, isCovariant = true,
            containerContext = memberContext,
            containerApplicabilityType =
            if (owner is FirJavaField) AnnotationQualifierApplicabilityType.FIELD
            else AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE,
            typeInSignature = TypeInSignature.Return
        ).enhance(session, jsr305State, predefinedEnhancementInfo?.returnTypeInfo)
        return signatureParts.type
    }

    private val overrideBindCache = mutableMapOf<Name, Map<FirCallableSymbol<*>?, List<FirCallableSymbol<*>>>>()

    private sealed class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableMemberDeclaration<*>): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration<*>): FirTypeRef = member.returnTypeRef
        }

        object Receiver : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration<*>): FirTypeRef {
                if (member is FirJavaMethod) return member.valueParameters[0].returnTypeRef
                return member.receiverTypeRef!!
            }
        }

        class ValueParameter(val hasReceiver: Boolean, val index: Int) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration<*>): FirTypeRef {
                if (hasReceiver && member is FirJavaMethod) {
                    return member.valueParameters[index + 1].returnTypeRef
                }
                return (member as FirFunction<*>).valueParameters[index].returnTypeRef
            }
        }
    }

    private fun FirFunction<*>.partsForValueParameter(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterContainer: FirAnnotationContainer?,
        methodContext: FirJavaEnhancementContext,
        typeInSignature: TypeInSignature
    ): EnhancementSignatureParts = (this as FirCallableMemberDeclaration<*>).parts(
        typeQualifierResolver,
        overriddenMembers,
        parameterContainer, false,
        parameterContainer?.let {
            methodContext.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, it.annotations)
        } ?: methodContext,
        AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
        typeInSignature
    )

    private fun FirCallableMemberDeclaration<*>.parts(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
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
                typeQualifierResolver, jsr305State, typeRef.annotations
            ),
            containerApplicabilityType
        )
    }
}
