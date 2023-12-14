// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isBuiltinFunctionClass
import org.jetbrains.kotlin.constant.StringValue
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Type
import org.jetbrains.kotlin.metadata.ProtoBuf.Type.Argument.Projection
import org.jetbrains.kotlin.metadata.ProtoBuf.TypeParameter.Variance
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.utils.doNothing

// TODO: see DescriptorRendererOptions.excludedTypeAnnotationClasses for decompiler
private val ANNOTATIONS_NOT_LOADED_FOR_TYPES = setOf(StandardNames.FqNames.parameterName)

const val COMPILED_DEFAULT_PARAMETER_VALUE = "COMPILED_CODE"

class TypeClsStubBuilder(private val c: ClsStubBuilderContext) {
    fun createTypeReferenceStub(
        parent: StubElement<out PsiElement>,
        type: Type,
        additionalAnnotations: () -> List<AnnotationWithTarget> = { emptyList() },
        loadTypeAnnotations: (Type) -> List<AnnotationWithArgs> = { c.components.annotationLoader.loadTypeAnnotations(it, c.nameResolver) }
    ) {
        val abbreviatedType = type.abbreviatedType(c.typeTable)
        if (abbreviatedType != null) {
            return createTypeReferenceStub(parent, abbreviatedType, additionalAnnotations)
        }

        val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(parent, KtStubElementTypes.TYPE_REFERENCE)

        val allAnnotationsInType = loadTypeAnnotations(type)
        val annotations = allAnnotationsInType.filterNot {
            val isTopLevelClass = !it.classId.isNestedClass
            isTopLevelClass && it.classId.asSingleFqName() in ANNOTATIONS_NOT_LOADED_FOR_TYPES
        }

        val allAnnotations = additionalAnnotations() + annotations.map { AnnotationWithTarget(it, null) }

        when {
            type.hasClassName() || type.hasTypeAliasName() ->
                createClassReferenceTypeStub(typeReference, type, allAnnotations)
            type.hasTypeParameter() ->
                createTypeParameterStub(typeReference, type, c.typeParameters[type.typeParameter], allAnnotations)
            type.hasTypeParameterName() ->
                createTypeParameterStub(typeReference, type, c.nameResolver.getName(type.typeParameterName), allAnnotations)
            else -> {
                doNothing()
            }
        }
    }

    private fun nullableTypeParent(parent: KotlinStubBaseImpl<*>, type: Type): KotlinStubBaseImpl<*> = if (type.nullable)
        KotlinPlaceHolderStubImpl<KtNullableType>(parent, KtStubElementTypes.NULLABLE_TYPE)
    else
        parent

    private fun createTypeParameterStub(parent: KotlinStubBaseImpl<*>, type: Type, name: Name, annotations: List<AnnotationWithTarget>) {
        createTypeAnnotationStubs(parent, type, annotations)
        val upperBoundType = if (type.hasFlexibleTypeCapabilitiesId()) {
            createKotlinTypeBean(type.flexibleUpperBound(c.typeTable)!!)
        } else null

        val typeParameterClassId = ClassId.topLevel(FqName.topLevel(name))
        if (Flags.DEFINITELY_NOT_NULL_TYPE.get(type.flags)) {
            createDefinitelyNotNullTypeStub(parent, typeParameterClassId, upperBoundType)
        } else {
            val nullableParentWrapper = nullableTypeParent(parent, type)
            createStubForTypeName(typeParameterClassId, nullableParentWrapper, { upperBoundType })
        }
    }

    private fun createDefinitelyNotNullTypeStub(parent: KotlinStubBaseImpl<*>, classId: ClassId, upperBoundType: KotlinTypeBean?) {
        val intersectionType = KotlinPlaceHolderStubImpl<KtIntersectionType>(parent, KtStubElementTypes.INTERSECTION_TYPE)
        val leftReference = KotlinPlaceHolderStubImpl<KtTypeReference>(intersectionType, KtStubElementTypes.TYPE_REFERENCE)
        createStubForTypeName(classId, leftReference, { upperBoundType })
        val rightReference = KotlinPlaceHolderStubImpl<KtTypeReference>(intersectionType, KtStubElementTypes.TYPE_REFERENCE)
        val userType = KotlinUserTypeStubImpl(rightReference)
        KotlinNameReferenceExpressionStubImpl(userType, StandardNames.FqNames.any.shortName().ref(), true)
    }

    private fun createClassReferenceTypeStub(parent: KotlinStubBaseImpl<*>, type: Type, annotations: List<AnnotationWithTarget>) {
        if (type.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(type.flexibleTypeCapabilitiesId)

            if (id == DYNAMIC_TYPE_DESERIALIZER_ID) {
                KotlinPlaceHolderStubImpl<KtDynamicType>(nullableTypeParent(parent, type), KtStubElementTypes.DYNAMIC_TYPE)
                return
            }
        }

        assert(type.hasClassName() || type.hasTypeAliasName()) {
            "Class reference stub must have either class or type alias name"
        }

        val classId = c.nameResolver.getClassId(if (type.hasClassName()) type.className else type.typeAliasName)
        val shouldBuildAsFunctionType = isBuiltinFunctionClass(classId) && type.argumentList.none { it.projection == Projection.STAR }
        if (shouldBuildAsFunctionType) {
            val (extensionAnnotations, notExtensionAnnotations) = annotations.partition {
                it.annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.extensionFunctionType
            }

            val (contextReceiverAnnotations, otherAnnotations) = notExtensionAnnotations.partition {
                it.annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.contextFunctionTypeParams
            }

            val isExtension = extensionAnnotations.isNotEmpty()
            val isSuspend = Flags.SUSPEND_TYPE.get(type.flags)

            val nullableWrapper = if (isSuspend) {
                val wrapper = nullableTypeParent(parent, type)
                createTypeAnnotationStubs(wrapper, type, otherAnnotations)
                wrapper
            } else {
                createTypeAnnotationStubs(parent, type, otherAnnotations)
                nullableTypeParent(parent, type)
            }

            val numContextReceivers = if (contextReceiverAnnotations.isEmpty()) {
                0
            } else {
                val argument = type.getExtension(JvmProtoBuf.typeAnnotation).find { c.nameResolver.getClassId(it.id).asSingleFqName() == StandardNames.FqNames.contextFunctionTypeParams }!!.getArgument(0)
                argument.value.intValue.toInt()
            }
            createFunctionTypeStub(nullableWrapper, type, isExtension, isSuspend, numContextReceivers)

            return
        }

        createTypeAnnotationStubs(parent, type, annotations)

        val outerTypeChain = generateSequence(type) { it.outerType(c.typeTable) }.toList()

        createStubForTypeName(classId, nullableTypeParent(parent, type), { level ->
            if (level == 0) createKotlinTypeBean(type.flexibleUpperBound(c.typeTable))
            else createKotlinTypeBean(outerTypeChain.getOrNull(level)?.flexibleUpperBound(c.typeTable))
        }) { userTypeStub, index ->
            outerTypeChain.getOrNull(index)?.let { createTypeArgumentListStub(userTypeStub, it.argumentList) }
        }
    }

    fun createKotlinTypeBean(
        type: Type?
    ): KotlinTypeBean? {
        if (type == null) return null
        val definitelyNotNull = Flags.DEFINITELY_NOT_NULL_TYPE.get(type.flags)
        val lowerBound = when {
            type.hasTypeParameter() -> {
                val lowerBound = KotlinTypeParameterTypeBean(
                    c.typeParameters[type.typeParameter].asString(),
                    type.nullable,
                    definitelyNotNull
                )
                lowerBound
            }
            type.hasTypeParameterName() -> {
                KotlinTypeParameterTypeBean(
                    c.nameResolver.getString(type.typeParameterName),
                    type.nullable,
                    definitelyNotNull
                )
            }
            else -> {
                val classId = c.nameResolver.getClassId(if (type.hasClassName()) type.className else type.typeAliasName)
                val arguments = type.argumentList.map { argument ->
                    val kind = argument.projection.toProjectionKind()
                    KotlinTypeArgumentBean(
                        kind,
                        if (kind == KtProjectionKind.STAR) null else createKotlinTypeBean(argument.type(c.typeTable))
                    )
                }
                KotlinClassTypeBean(classId, arguments, type.nullable)
            }
        }
        val upperBoundBean = createKotlinTypeBean(type.flexibleUpperBound(c.typeTable))
        return if (upperBoundBean != null) {
            KotlinFlexibleTypeBean(lowerBound, upperBoundBean)
        } else lowerBound
    }

    private fun createTypeAnnotationStubs(parent: KotlinStubBaseImpl<*>, type: Type, annotations: List<AnnotationWithTarget>) {
        val typeModifiers = getTypeModifiersAsWritten(type)
        if (annotations.isEmpty() && typeModifiers.isEmpty()) return
        val typeModifiersMask = ModifierMaskUtils.computeMask { it in typeModifiers }
        val modifiersList = KotlinModifierListStubImpl(parent, typeModifiersMask, KtStubElementTypes.MODIFIER_LIST)
        createTargetedAnnotationStubs(annotations, modifiersList)
    }

    private fun getTypeModifiersAsWritten(type: Type): Set<KtModifierKeywordToken> {
        if (!type.hasClassName() && !type.hasTypeAliasName()) return emptySet()

        val result = hashSetOf<KtModifierKeywordToken>()

        if (Flags.SUSPEND_TYPE.get(type.flags)) {
            result.add(KtTokens.SUSPEND_KEYWORD)
        }

        return result
    }

    private fun createTypeArgumentListStub(typeStub: KotlinUserTypeStub, typeArgumentProtoList: List<Type.Argument>) {
        if (typeArgumentProtoList.isEmpty()) {
            return
        }
        val typeArgumentsListStub = KotlinPlaceHolderStubImpl<KtTypeArgumentList>(typeStub, KtStubElementTypes.TYPE_ARGUMENT_LIST)
        typeArgumentProtoList.forEach { typeArgumentProto ->
            val projectionKind = typeArgumentProto.projection.toProjectionKind()
            val typeProjection = KotlinTypeProjectionStubImpl(typeArgumentsListStub, projectionKind.ordinal)
            if (projectionKind != KtProjectionKind.STAR) {
                val modifierKeywordToken = projectionKind.token as? KtModifierKeywordToken
                createModifierListStub(typeProjection, listOfNotNull(modifierKeywordToken))
                createTypeReferenceStub(typeProjection, typeArgumentProto.type(c.typeTable)!!)
            }
        }
    }

    private fun Projection.toProjectionKind() = when (this) {
        Projection.IN -> KtProjectionKind.IN
        Projection.OUT -> KtProjectionKind.OUT
        Projection.INV -> KtProjectionKind.NONE
        Projection.STAR -> KtProjectionKind.STAR
    }

    private fun createFunctionTypeStub(
        parent: StubElement<out PsiElement>,
        type: Type,
        isExtensionFunctionType: Boolean,
        isSuspend: Boolean,
        numContextReceivers: Int,
    ) {
        val typeArgumentList = type.argumentList
        val functionType = KotlinPlaceHolderStubImpl<KtFunctionType>(parent, KtStubElementTypes.FUNCTION_TYPE)
        var processedTypes = 0

        if (numContextReceivers != 0) {
            ContextReceiversListStubBuilder(c).createContextReceiverStubs(
                functionType,
                typeArgumentList.subList(
                    processedTypes,
                    processedTypes + numContextReceivers
                ).map { it.type(c.typeTable)!! })
            processedTypes += numContextReceivers
        }

        if (isExtensionFunctionType) {
            val functionTypeReceiverStub =
                KotlinPlaceHolderStubImpl<KtFunctionTypeReceiver>(functionType, KtStubElementTypes.FUNCTION_TYPE_RECEIVER)
            val receiverTypeProto = typeArgumentList[processedTypes].type(c.typeTable)!!
            createTypeReferenceStub(functionTypeReceiverStub, receiverTypeProto)
            processedTypes++
        }

        val parameterList = KotlinPlaceHolderStubImpl<KtParameterList>(functionType, KtStubElementTypes.VALUE_PARAMETER_LIST)
        val typeArgumentsWithoutReceiverAndReturnType = typeArgumentList.subList(processedTypes, typeArgumentList.size - 1)
        var suspendParameterType: Type? = null

        for ((index, argument) in typeArgumentsWithoutReceiverAndReturnType.withIndex()) {
            val parameterType = argument.type(c.typeTable)!!
            if (isSuspend && index == typeArgumentsWithoutReceiverAndReturnType.size - 1) {
                if (parameterType.hasClassName() && parameterType.argumentCount == 1) {
                    val classId = c.nameResolver.getClassId(parameterType.className)
                    val fqName = classId.asSingleFqName()
                    assert(
                        fqName == FqName("kotlin.coroutines.Continuation") ||
                                fqName == FqName("kotlin.coroutines.experimental.Continuation")
                    ) {
                        "Last parameter type of suspend function must be Continuation, but it is $fqName"
                    }
                    suspendParameterType = parameterType
                    continue
                }
            }
            val annotations = c.components.annotationLoader.loadTypeAnnotations(parameterType, c.nameResolver)

            fun getFunctionTypeParameterName(annotations: List<AnnotationWithArgs>): String? {
                for (annotationWithArgs in annotations) {
                    if (annotationWithArgs.classId.asSingleFqName() == StandardNames.FqNames.parameterName) {
                        return (annotationWithArgs.args.values.firstOrNull() as? StringValue)?.value
                    }
                }
                return null
            }

            val parameter = KotlinParameterStubImpl(
                parameterList,
                fqName = null,
                name = null,
                isMutable = false,
                hasValOrVar = false,
                hasDefaultValue = false,
                functionTypeParameterName = getFunctionTypeParameterName(annotations)
            )
            createTypeReferenceStub(parameter, parameterType, loadTypeAnnotations = { annotations })
        }


        if (suspendParameterType == null) {
            val returnType = typeArgumentList.last().type(c.typeTable)!!
            createTypeReferenceStub(functionType, returnType)
        } else {
            val continuationArgumentType = suspendParameterType.getArgument(0).type(c.typeTable)!!
            createTypeReferenceStub(functionType, continuationArgumentType)
        }
    }

    fun createValueParameterListStub(
        parent: StubElement<out PsiElement>,
        callableProto: MessageLite,
        parameters: List<ProtoBuf.ValueParameter>,
        container: ProtoContainer,
        callableKind: AnnotatedCallableKind = callableProto.annotatedCallableKind
    ) {
        val parameterListStub = KotlinPlaceHolderStubImpl<KtParameterList>(parent, KtStubElementTypes.VALUE_PARAMETER_LIST)
        for ((index, valueParameterProto) in parameters.withIndex()) {
            val parameterName = computeParameterName(c.nameResolver.getName(valueParameterProto.name))
            val hasDefaultValue = Flags.DECLARES_DEFAULT_VALUE.get(valueParameterProto.flags)
            val parameterStub = KotlinParameterStubImpl(
                parameterListStub,
                name = parameterName.ref(),
                fqName = null,
                hasDefaultValue = hasDefaultValue,
                hasValOrVar = false,
                isMutable = false
            )
            val varargElementType = valueParameterProto.varargElementType(c.typeTable)
            val typeProto = varargElementType ?: valueParameterProto.type(c.typeTable)
            val modifiers = arrayListOf<KtModifierKeywordToken>()

            if (varargElementType != null) {
                modifiers.add(KtTokens.VARARG_KEYWORD)
            }
            if (Flags.IS_CROSSINLINE.get(valueParameterProto.flags)) {
                modifiers.add(KtTokens.CROSSINLINE_KEYWORD)
            }
            if (Flags.IS_NOINLINE.get(valueParameterProto.flags)) {
                modifiers.add(KtTokens.NOINLINE_KEYWORD)
            }

            val modifierList = createModifierListStub(parameterStub, modifiers)

            if (Flags.HAS_ANNOTATIONS.get(valueParameterProto.flags)) {
                val parameterAnnotations = c.components.annotationLoader.loadValueParameterAnnotations(
                    container, callableProto, callableKind, index, valueParameterProto
                )
                if (parameterAnnotations.isNotEmpty()) {
                    createAnnotationStubs(parameterAnnotations, modifierList ?: createEmptyModifierListStub(parameterStub))
                }
            }

            createTypeReferenceStub(parameterStub, typeProto)
            if (hasDefaultValue) {
                KotlinNameReferenceExpressionStubImpl(parameterStub, StringRef.fromString(COMPILED_DEFAULT_PARAMETER_VALUE))
            }
        }
    }

    fun createTypeParameterListStub(
        parent: StubElement<out PsiElement>,
        typeParameterProtoList: List<ProtoBuf.TypeParameter>
    ): List<Pair<Name, Type>> {
        if (typeParameterProtoList.isEmpty()) return listOf()

        val typeParameterListStub = KotlinPlaceHolderStubImpl<KtTypeParameterList>(parent, KtStubElementTypes.TYPE_PARAMETER_LIST)
        val protosForTypeConstraintList = arrayListOf<Pair<Name, Type>>()
        for (proto in typeParameterProtoList) {
            val name = c.nameResolver.getName(proto.name)
            val typeParameterStub = KotlinTypeParameterStubImpl(
                typeParameterListStub,
                name = name.ref(),
                isInVariance = proto.variance == Variance.IN,
                isOutVariance = proto.variance == Variance.OUT
            )
            createTypeParameterModifierListStub(typeParameterStub, proto)
            val upperBoundProtos = proto.upperBounds(c.typeTable)
            if (upperBoundProtos.isNotEmpty()) {
                val upperBound = upperBoundProtos.first()
                if (!upperBound.isDefaultUpperBound()) {
                    createTypeReferenceStub(typeParameterStub, upperBound)
                }
                protosForTypeConstraintList.addAll(upperBoundProtos.drop(1).map { Pair(name, it) })
            }
        }
        return protosForTypeConstraintList
    }

    fun createTypeConstraintListStub(
        parent: StubElement<out PsiElement>,
        protosForTypeConstraintList: List<Pair<Name, Type>>
    ) {
        if (protosForTypeConstraintList.isEmpty()) {
            return
        }
        val typeConstraintListStub = KotlinPlaceHolderStubImpl<KtTypeConstraintList>(parent, KtStubElementTypes.TYPE_CONSTRAINT_LIST)
        for ((name, type) in protosForTypeConstraintList) {
            val typeConstraintStub = KotlinPlaceHolderStubImpl<KtTypeConstraint>(typeConstraintListStub, KtStubElementTypes.TYPE_CONSTRAINT)
            KotlinNameReferenceExpressionStubImpl(typeConstraintStub, name.ref())
            createTypeReferenceStub(typeConstraintStub, type)
        }
    }

    private fun createTypeParameterModifierListStub(
        typeParameterStub: KotlinTypeParameterStubImpl,
        typeParameterProto: ProtoBuf.TypeParameter
    ) {
        val modifiers = ArrayList<KtModifierKeywordToken>()
        when (typeParameterProto.variance) {
            Variance.IN -> modifiers.add(KtTokens.IN_KEYWORD)
            Variance.OUT -> modifiers.add(KtTokens.OUT_KEYWORD)
            Variance.INV -> { /* do nothing */
            }
            null -> { /* do nothing */
            }
        }
        if (typeParameterProto.reified) {
            modifiers.add(KtTokens.REIFIED_KEYWORD)
        }

        val modifierList = createModifierListStub(typeParameterStub, modifiers)

        val annotations = c.components.annotationLoader.loadTypeParameterAnnotations(typeParameterProto, c.nameResolver)
        if (annotations.isNotEmpty()) {
            createAnnotationStubs(
                annotations,
                modifierList ?: createEmptyModifierListStub(typeParameterStub)
            )
        }
    }

    private fun Type.isDefaultUpperBound(): Boolean {
        return this.hasClassName() &&
                c.nameResolver.getClassId(className).let { StandardNames.FqNames.any == it.asSingleFqName().toUnsafe() } &&
                this.nullable
    }
}
