/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.typeEnhancement.PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE
import org.jetbrains.kotlin.load.java.typeEnhancement.PredefinedFunctionEnhancementInfo
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Jsr305State

class JavaClassEnhancementScope(
    private val session: FirSession,
    private val useSiteMemberScope: JavaClassUseSiteMemberScope
) : FirScope() {
    private val owner: FirRegularClass = useSiteMemberScope.symbol.fir

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (owner is FirJavaClass) owner.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private val jsr305State: Jsr305State = session.jsr305State ?: Jsr305State.DEFAULT

    private val typeQualifierResolver = FirAnnotationTypeQualifierResolver(session, jsr305State)

    private val context: FirJavaEnhancementContext =
        FirJavaEnhancementContext(session) { null }.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, owner.annotations)

    private val enhancements = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>>()

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) process@{ original ->

            val field = enhancements.getOrPut(original) { enhance(original, name) }
            processor(field)
        }

        return super.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->

            val function = enhancements.getOrPut(original) { enhance(original, name) }
            processor(function as FirFunctionSymbol<*>)
        }

        return super.processFunctionsByName(name, processor)
    }

    override fun processClassifiersByName(name: Name, processor: (FirClassifierSymbol<*>) -> Unit) {
        useSiteMemberScope.processClassifiersByName(name, processor)
    }

    private fun enhance(
        original: FirCallableSymbol<*>,
        name: Name
    ): FirCallableSymbol<*> {
        when (val firElement = original.fir) {
            is FirField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firElement.annotations)
                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, null)

                val symbol = FirFieldSymbol(original.callableId)
                with(firElement) {
                    FirJavaField(
                        firElement.source,
                        this@JavaClassEnhancementScope.session,
                        symbol,
                        name,
                        visibility,
                        modality,
                        newReturnTypeRef,
                        isVar,
                        isStatic
                    ).apply {
                        annotations += firElement.annotations
                    }
                }
                return symbol
            }
            is FirJavaMethod -> {
                original as FirAccessorSymbol
                return enhanceMethod(
                    firElement, original.accessorId, original.accessorId.callableName, isAccessor = true, propertyId = original.callableId
                ) as FirAccessorSymbol
            }
            else -> {
                if (original is FirPropertySymbol || original is FirAccessorSymbol) return original
                error("Can't make enhancement for $original: `${firElement.render()}`")
            }
        }
    }

    private fun enhance(
        original: FirFunctionSymbol<*>,
        name: Name
    ): FirFunctionSymbol<*> {
        val firMethod = original.fir

        if (firMethod !is FirJavaMethod && firMethod !is FirJavaConstructor) {
            return original
        }
        if (firMethod !is FirMemberFunction<*>) throw AssertionError()
        return enhanceMethod(firMethod, original.callableId, name)
    }

    private fun enhanceMethod(
        firMethod: FirMemberFunction<*>,
        methodId: CallableId,
        name: Name,
        isAccessor: Boolean = false,
        propertyId: CallableId? = null
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

        val overriddenMembers = firMethod.overriddenMembers()
        val hasReceiver = overriddenMembers.any { it.receiverTypeRef != null }

        val newReceiverTypeRef = if (firMethod is FirJavaMethod && hasReceiver) {
            enhanceReceiverType(firMethod, overriddenMembers, memberContext)
        } else null
        val newReturnTypeRef = if (firMethod is FirJavaConstructor) {
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
            with(valueParameter) {
                FirValueParameterImpl(
                    source, this@JavaClassEnhancementScope.session,
                    newTypeRef, this.name,
                    FirVariableSymbol(this.name),
                    defaultValue ?: newDefaultValue, isCrossinline, isNoinline, isVararg
                ).apply {
                    resolvePhase = FirResolvePhase.DECLARATIONS
                    annotations += valueParameter.annotations
                }
            }
        }
        val function = when (firMethod) {
            is FirJavaConstructor -> {
                val symbol = FirConstructorSymbol(methodId)
                val status = FirDeclarationStatusImpl(firMethod.visibility, Modality.FINAL).apply {
                    isExpect = false
                    isActual = false
                    isInner = firMethod.isInner
                }
                if (firMethod.isPrimary) {
                    FirPrimaryConstructorImpl(
                        firMethod.source,
                        this@JavaClassEnhancementScope.session,
                        newReturnTypeRef,
                        null,
                        status,
                        symbol
                    )
                } else {
                    FirConstructorImpl(
                        firMethod.source,
                        this@JavaClassEnhancementScope.session,
                        newReturnTypeRef,
                        newReceiverTypeRef,
                        firMethod.status,
                        symbol
                    )
                }.apply {
                    resolvePhase = FirResolvePhase.DECLARATIONS
                    this.valueParameters += newValueParameters
                    this.typeParameters += firMethod.typeParameters
                }
            }
            else -> FirSimpleFunctionImpl(
                firMethod.source,
                this@JavaClassEnhancementScope.session,
                newReturnTypeRef,
                newReceiverTypeRef,
                name,
                firMethod.status,
                if (!isAccessor) FirNamedFunctionSymbol(methodId)
                else FirAccessorSymbol(callableId = propertyId!!, accessorId = methodId)
            ).apply {
                resolvePhase = FirResolvePhase.DECLARATIONS
                this.valueParameters += newValueParameters
                this.typeParameters += firMethod.typeParameters
            }
        }
        function.annotations += firMethod.annotations
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
        ownerFunction: FirCallableMemberDeclaration<*>,
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
        ).enhance(session, jsr305State, predefinedEnhancementInfo?.parametersInfo?.getOrNull(index))
        val firResolvedTypeRef = signatureParts.type
        val defaultValueExpression = when (val defaultValue = ownerParameter.getDefaultValueFromAnnotation()) {
            NullDefaultValue -> FirConstExpressionImpl(null, FirConstKind.Null, null)
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
            if (owner is FirJavaField) AnnotationTypeQualifierResolver.QualifierApplicabilityType.FIELD
            else AnnotationTypeQualifierResolver.QualifierApplicabilityType.METHOD_RETURN_TYPE,
            typeInSignature = TypeInSignature.Return
        ).enhance(session, jsr305State, predefinedEnhancementInfo?.returnTypeInfo)
        return signatureParts.type
    }

    private val overrideBindCache = mutableMapOf<Name, Map<FirCallableSymbol<*>?, List<FirCallableSymbol<*>>>>()

    private fun FirCallableMemberDeclaration<*>.overriddenMembers(): List<FirCallableMemberDeclaration<*>> {
        val backMap = overrideBindCache.getOrPut(this.name) {
            useSiteMemberScope.bindOverrides(this.name)
            useSiteMemberScope
                .overrideByBase
                .toList()
                .groupBy({ (_, key) -> key }, { (value) -> value })
        }
        return backMap[this.symbol]?.map { it.fir as FirCallableMemberDeclaration<*> } ?: emptyList()
    }

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

    private fun FirCallableMemberDeclaration<*>.partsForValueParameter(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterContainer: FirAnnotationContainer?,
        methodContext: FirJavaEnhancementContext,
        typeInSignature: TypeInSignature
    ) = parts(
        typeQualifierResolver,
        overriddenMembers,
        parameterContainer, false,
        parameterContainer?.let {
            methodContext.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, it.annotations)
        } ?: methodContext,
        AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER,
        typeInSignature
    )

    private fun FirCallableMemberDeclaration<*>.parts(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration<*>>,
        typeContainer: FirAnnotationContainer?,
        isCovariant: Boolean,
        containerContext: FirJavaEnhancementContext,
        containerApplicabilityType: AnnotationTypeQualifierResolver.QualifierApplicabilityType,
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