/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.*
import org.jetbrains.kotlin.fir.java.enhancement.EnhancementSignatureParts
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.typeEnhancement.PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE
import org.jetbrains.kotlin.load.java.typeEnhancement.PredefinedFunctionEnhancementInfo
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Jsr305State

class JavaClassEnhancementScope(
    private val session: FirSession,
    private val useSiteScope: JavaClassUseSiteScope
) : FirScope {
    private val owner: FirRegularClass = useSiteScope.symbol.fir

    private val javaTypeParameterStack: JavaTypeParameterStack =
        if (owner is FirJavaClass) owner.javaTypeParameterStack else JavaTypeParameterStack.EMPTY

    private val jsr305State: Jsr305State = session.jsr305State ?: Jsr305State.DEFAULT

    private val typeQualifierResolver = FirAnnotationTypeQualifierResolver(session, jsr305State)

    private val context: FirJavaEnhancementContext =
        FirJavaEnhancementContext(session) { null }.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, owner.annotations)

    private val enhancements = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol>()

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        useSiteScope.processPropertiesByName(name) process@{ original ->

            val field = enhancements.getOrPut(original) { enhance(original, name) }
            processor(field as ConeVariableSymbol)
        }

        return super.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        useSiteScope.processFunctionsByName(name) process@{ original ->

            val function = enhancements.getOrPut(original) { enhance(original, name) }
            processor(function as ConeFunctionSymbol)
        }

        return super.processFunctionsByName(name, processor)
    }

    private fun enhance(
        original: ConeVariableSymbol,
        name: Name
    ): ConeVariableSymbol {
        when (val firElement = (original as FirBasedSymbol<*>).fir) {
            is FirJavaField -> {
                if (firElement.returnTypeRef !is FirJavaTypeRef) return original
                val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firElement.annotations)
                val newReturnTypeRef = enhanceReturnType(firElement, emptyList(), memberContext, null)

                val symbol = FirPropertySymbol(original.callableId)
                with(firElement) {
                    FirJavaField(
                        session,
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
                if (original is ConePropertySymbol) return original
                error("Can't make enhancement for $original: `${firElement.render()}`")
            }
        }
    }

    private fun enhance(
        original: ConeFunctionSymbol,
        name: Name
    ): FirFunctionSymbol {
        val firMethod = (original as FirFunctionSymbol).fir as? FirFunction

        if (firMethod !is FirJavaMethod && firMethod !is FirJavaConstructor || firMethod !is FirCallableMemberDeclaration) return original
        return enhanceMethod(firMethod, original.callableId, name) as FirFunctionSymbol
    }

    private fun enhanceMethod(
        firMethod: FirFunction,
        methodId: CallableId,
        name: Name,
        isAccessor: Boolean = false,
        propertyId: CallableId? = null
    ): ConeCallableSymbol {
        require(firMethod is FirCallableMemberDeclaration)
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

        val symbol = if (!isAccessor) FirFunctionSymbol(methodId) else FirAccessorSymbol(callableId = propertyId!!, accessorId = methodId)
        val newValueParameters = firMethod.valueParameters.zip(newValueParameterInfo) { valueParameter, newInfo ->
            val (newTypeRef, newDefaultValue) = newInfo
            with(valueParameter) {
                FirValueParameterImpl(
                    this@JavaClassEnhancementScope.session, psi,
                    this.name, newTypeRef,
                    defaultValue ?: newDefaultValue, isCrossinline, isNoinline, isVararg
                ).apply {
                    annotations += valueParameter.annotations
                }
            }
        }
        val function = when (firMethod) {
            is FirJavaConstructor -> FirConstructorImpl(
                this@JavaClassEnhancementScope.session, null, symbol as FirFunctionSymbol,
                newReceiverTypeRef, newReturnTypeRef
            ).apply {
                this.valueParameters += newValueParameters
                this.typeParameters += firMethod.typeParameters
            }
            else -> FirMemberFunctionImpl(
                this@JavaClassEnhancementScope.session, null, symbol, name,
                newReceiverTypeRef, newReturnTypeRef
            ).apply {
                this.valueParameters += newValueParameters
                this.typeParameters += firMethod.typeParameters
            }
        }
        function.apply {
            status = firMethod.status as FirDeclarationStatusImpl
            annotations += firMethod.annotations
        }
        return symbol
    }

    private fun FirFunction.computeJvmDescriptor(): String = buildString {
        if (this@computeJvmDescriptor is FirJavaMethod) {
            append(name.asString())
        } else {
            append("<init>")
        }

        append("(")
        for (parameter in valueParameters) {
            appendErasedType(parameter.returnTypeRef)
        }
        append(")")

        if (this@computeJvmDescriptor !is FirJavaMethod || (returnTypeRef as FirJavaTypeRef).isVoid()) {
            append("V")
        } else {
            appendErasedType(returnTypeRef)
        }
    }

    private fun StringBuilder.appendErasedType(typeRef: FirTypeRef) {
        when (typeRef) {
            is FirResolvedTypeRef -> appendConeType(typeRef.type)
            is FirJavaTypeRef -> appendConeType(typeRef.toNotNullConeKotlinType(session, javaTypeParameterStack))
        }
    }

    private fun StringBuilder.appendConeType(coneType: ConeKotlinType) {
        if (coneType is ConeClassErrorType) return
        append("L")
        when (coneType) {
            is ConeClassLikeType -> {
                val classId = coneType.lookupTag.classId
                append(classId.packageFqName.asString().replace(".", "/"))
                append("/")
                append(classId.relativeClassName)
            }
            is ConeTypeParameterType -> append(coneType.lookupTag.name)
        }
        append(";")
    }

    private fun FirJavaTypeRef.isVoid(): Boolean {
        return type is JavaPrimitiveType && type.type == null
    }

    // ================================================================================================

    private fun enhanceReceiverType(
        ownerFunction: FirJavaMethod,
        overriddenMembers: List<FirCallableMemberDeclaration>,
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
        ownerFunction: FirCallableMemberDeclaration,
        overriddenMembers: List<FirCallableMemberDeclaration>,
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
            NullDefaultValue -> FirConstExpressionImpl(session, null, IrConstKind.Null, null)
            is StringDefaultValue -> firResolvedTypeRef.type.lexicalCastFrom(session, defaultValue.value)
            null -> null
        }
        return EnhanceValueParameterResult(firResolvedTypeRef, defaultValueExpression)
    }

    private fun enhanceReturnType(
        owner: FirCallableMemberDeclaration,
        overriddenMembers: List<FirCallableMemberDeclaration>,
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

    private val overrideBindCache = mutableMapOf<Name, Map<ConeFunctionSymbol?, List<ConeCallableSymbol>>>()

    private fun FirCallableMemberDeclaration.overriddenMembers(): List<FirCallableMemberDeclaration> {
        val backMap = overrideBindCache.getOrPut(this.name) {
            useSiteScope.bindOverrides(this.name)
            useSiteScope
                .overriddenByBase
                .toList()
                .groupBy({ (_, key) -> key }, { (value) -> value })
        }
        return backMap[this.symbol]?.map { it.firUnsafe() } ?: emptyList()
    }

    private sealed class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableMemberDeclaration): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration): FirTypeRef = member.returnTypeRef
        }

        object Receiver : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration): FirTypeRef {
                if (member is FirJavaMethod) return member.valueParameters[0].returnTypeRef
                return member.receiverTypeRef!!
            }
        }

        class ValueParameter(val hasReceiver: Boolean, val index: Int) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMemberDeclaration): FirTypeRef {
                if (hasReceiver && member is FirJavaMethod) {
                    return (member as FirFunction).valueParameters[index + 1].returnTypeRef
                }
                return (member as FirFunction).valueParameters[index].returnTypeRef
            }
        }
    }

    private fun FirCallableMemberDeclaration.partsForValueParameter(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration>,
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

    private fun FirCallableMemberDeclaration.parts(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMemberDeclaration>,
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