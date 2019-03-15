/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.*
import org.jetbrains.kotlin.fir.java.enhancement.EnhancementSignatureParts
import org.jetbrains.kotlin.fir.java.toNotNullConeKotlinType
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
    private val owner: FirRegularClass get() = useSiteScope.symbol.fir

    private val jsr305State: Jsr305State = session.jsr305State ?: Jsr305State.DEFAULT

    private val typeQualifierResolver = FirAnnotationTypeQualifierResolver(session, jsr305State)

    private val context: FirJavaEnhancementContext =
        FirJavaEnhancementContext(session) { null }.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, owner.annotations)

    private val enhancements = mutableMapOf<ConeCallableSymbol, ConeCallableSymbol>()

    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
        useSiteScope.processPropertiesByName(name) process@{ original ->

            val field = enhancements.getOrPut(original) { enhance(original, name) }
            processor(field as ConePropertySymbol)
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
        original: ConePropertySymbol,
        name: Name
    ): FirPropertySymbol {
        val firField = (original as FirBasedSymbol<*>).fir as? FirJavaField ?: error("Can't make enhancement for $original")

        val memberContext = context.copyWithNewDefaultTypeQualifiers(typeQualifierResolver, jsr305State, firField.annotations)
        val newReturnTypeRef = enhanceReturnType(firField, emptyList(), memberContext, null)

        val symbol = FirPropertySymbol(original.callableId)
        with(firField) {
            FirMemberPropertyImpl(
                this@JavaClassEnhancementScope.session, null, symbol, name,
                visibility, modality, isExpect, isActual, isOverride,
                isConst = false, isLateInit = false,
                receiverTypeRef = null,
                returnTypeRef = newReturnTypeRef,
                isVar = isVar, initializer = null,
                getter = FirDefaultPropertyGetter(this@JavaClassEnhancementScope.session, null, newReturnTypeRef, visibility),
                setter = FirDefaultPropertySetter(this@JavaClassEnhancementScope.session, null, newReturnTypeRef, visibility),
                delegate = null
            ).apply {
                annotations += firField.annotations
                status.isStatic = firField.isStatic
            }
        }
        return symbol
    }

    private fun enhance(
        original: ConeFunctionSymbol,
        name: Name
    ): FirFunctionSymbol {
        val firMethod = (original as FirFunctionSymbol).fir as? FirFunction

        if (firMethod !is FirJavaMethod && firMethod !is FirJavaConstructor || firMethod !is FirCallableMember) return original

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

        val symbol = FirFunctionSymbol(original.callableId)
        FirMemberFunctionImpl(
            this@JavaClassEnhancementScope.session, null, symbol, name,
            newReceiverTypeRef, newReturnTypeRef
        ).apply {
            status = firMethod.status as FirDeclarationStatusImpl
            annotations += firMethod.annotations
            valueParameters += firMethod.valueParameters.zip(newValueParameterInfo) { valueParameter, newInfo ->
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
            is FirJavaTypeRef -> appendConeType(typeRef.toNotNullConeKotlinType(session))
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
        overriddenMembers: List<FirCallableMember>,
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
        ownerFunction: FirCallableMember,
        overriddenMembers: List<FirCallableMember>,
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
        val defaultValue = ownerParameter.getDefaultValueFromAnnotation()
        val defaultValueExpression = when (defaultValue) {
            NullDefaultValue -> FirConstExpressionImpl(session, null, IrConstKind.Null, null)
            is StringDefaultValue -> firResolvedTypeRef.type.lexicalCastFrom(session, defaultValue.value)
            null -> null
        }
        return EnhanceValueParameterResult(firResolvedTypeRef, defaultValueExpression)
    }

    private fun enhanceReturnType(
        owner: FirCallableMember,
        overriddenMembers: List<FirCallableMember>,
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

    private val overriddenMemberCache = mutableMapOf<FirCallableMember, List<FirCallableMember>>()

    private fun FirCallableMember.overriddenMembers(): List<FirCallableMember> {
        return overriddenMemberCache.getOrPut(this) {
            val result = mutableListOf<FirCallableMember>()
            if (this is FirNamedFunction) {
                val superTypesScope = useSiteScope.superTypesScope
                superTypesScope.processFunctionsByName(this.name) { basicFunctionSymbol ->
                    val overriddenBy = with(useSiteScope) {
                        basicFunctionSymbol.getOverridden(setOf(this@overriddenMembers.symbol as ConeFunctionSymbol))
                    }
                    val overriddenByFir = (overriddenBy as? FirFunctionSymbol)?.fir
                    if (overriddenByFir === this@overriddenMembers) {
                        result += (basicFunctionSymbol as FirFunctionSymbol).fir
                    }
                    ProcessorAction.NEXT
                }
            }
            result
        }
    }

    private sealed class TypeInSignature {
        abstract fun getTypeRef(member: FirCallableMember): FirTypeRef

        object Return : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMember): FirTypeRef = member.returnTypeRef
        }

        object Receiver : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMember): FirTypeRef {
                if (member is FirJavaMethod) return member.valueParameters[0].returnTypeRef
                return member.receiverTypeRef!!
            }
        }

        class ValueParameter(val hasReceiver: Boolean, val index: Int) : TypeInSignature() {
            override fun getTypeRef(member: FirCallableMember): FirTypeRef {
                if (hasReceiver && member is FirJavaMethod) {
                    return (member as FirFunction).valueParameters[index + 1].returnTypeRef
                }
                return (member as FirFunction).valueParameters[index].returnTypeRef
            }
        }
    }

    private fun FirCallableMember.partsForValueParameter(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMember>,
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

    private fun FirCallableMember.parts(
        typeQualifierResolver: FirAnnotationTypeQualifierResolver,
        overriddenMembers: List<FirCallableMember>,
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