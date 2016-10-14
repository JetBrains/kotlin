/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.ANNOTATIONS_COPIED_TO_TYPES
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Type
import org.jetbrains.kotlin.serialization.ProtoBuf.Type.Argument.Projection
import org.jetbrains.kotlin.serialization.ProtoBuf.TypeParameter.Variance
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

// TODO: see DescriptorRendererOptions.excludedTypeAnnotationClasses for decompiler
private val ANNOTATIONS_NOT_LOADED_FOR_TYPES = (ANNOTATIONS_COPIED_TO_TYPES + KotlinBuiltIns.FQ_NAMES.parameterName).toSet()

class TypeClsStubBuilder(private val c: ClsStubBuilderContext) {

    fun createTypeReferenceStub(parent: StubElement<out PsiElement>, type: Type) {
        if (type.hasAbbreviatedType()) return createTypeReferenceStub(parent, type.abbreviatedType)
        val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(parent, KtStubElementTypes.TYPE_REFERENCE)

        val annotations = c.components.annotationLoader.loadTypeAnnotations(type, c.nameResolver).filterNot {
            val isTopLevelClass = !it.isNestedClass
            isTopLevelClass && it.asSingleFqName() in ANNOTATIONS_NOT_LOADED_FOR_TYPES
        }

        val effectiveParent =
                if (type.nullable) KotlinPlaceHolderStubImpl<KtNullableType>(typeReference, KtStubElementTypes.NULLABLE_TYPE)
                else typeReference

        fun createTypeParameterStub(name: Name) {
            createTypeAnnotationStubs(effectiveParent, annotations)
            createStubForTypeName(ClassId.topLevel(FqName.topLevel(name)), effectiveParent)
        }

        when {
            type.hasClassName() || type.hasTypeAliasName() -> createClassReferenceTypeStub(effectiveParent, type, annotations)
            type.hasTypeParameter() -> createTypeParameterStub(c.typeParameters[type.typeParameter])
            type.hasTypeParameterName() -> createTypeParameterStub(c.nameResolver.getName(type.typeParameterName))
        }
    }

    private fun createClassReferenceTypeStub(parent: KotlinStubBaseImpl<*>, type: Type, annotations: List<ClassId>) {
        if (type.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(type.flexibleTypeCapabilitiesId)

            if (id == DynamicTypeDeserializer.id) {
                KotlinPlaceHolderStubImpl<KtDynamicType>(parent, KtStubElementTypes.DYNAMIC_TYPE)
                return
            }
        }

        assert(type.hasClassName() || type.hasTypeAliasName()) {
            "Class reference stub must have either class or type alias name"
        }

        val classId = c.nameResolver.getClassId(if (type.hasClassName()) type.className else type.typeAliasName)
        val shouldBuildAsFunctionType = isNumberedFunctionClassFqName(classId.asSingleFqName().toUnsafe())
                                        && type.argumentList.none { it.projection == Projection.STAR }
        if (shouldBuildAsFunctionType) {
            val extension = annotations.any { annotation ->
                annotation.asSingleFqName() == KotlinBuiltIns.FQ_NAMES.extensionFunctionType
            }
            createFunctionTypeStub(parent, type, extension)
            return
        }
        createTypeAnnotationStubs(parent, annotations)
        val outerTypeChain = generateSequence(type) { it.outerType(c.typeTable) }.toList()

        createStubForTypeName(classId, parent) {
            userTypeStub, index ->
            outerTypeChain.getOrNull(index)?.let { createTypeArgumentListStub(userTypeStub, it.argumentList) }
        }
    }

    private fun createTypeAnnotationStubs(parent: KotlinStubBaseImpl<*>, annotations: List<ClassId>) {
        createAnnotationStubs(annotations, parent)
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
                createModifierListStub(typeProjection, modifierKeywordToken.singletonOrEmptyList())
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

    private fun createFunctionTypeStub(parent: StubElement<out PsiElement>, type: Type, isExtensionFunctionType: Boolean) {
        val typeArgumentList = type.argumentList
        val functionType = KotlinPlaceHolderStubImpl<KtFunctionType>(parent, KtStubElementTypes.FUNCTION_TYPE)
        if (isExtensionFunctionType) {
            val functionTypeReceiverStub
                    = KotlinPlaceHolderStubImpl<KtFunctionTypeReceiver>(functionType, KtStubElementTypes.FUNCTION_TYPE_RECEIVER)
            val receiverTypeProto = typeArgumentList.first().type(c.typeTable)!!
            createTypeReferenceStub(functionTypeReceiverStub, receiverTypeProto)
        }

        val parameterList = KotlinPlaceHolderStubImpl<KtParameterList>(functionType, KtStubElementTypes.VALUE_PARAMETER_LIST)
        val typeArgumentsWithoutReceiverAndReturnType
                = typeArgumentList.subList(if (isExtensionFunctionType) 1 else 0, typeArgumentList.size - 1)
        typeArgumentsWithoutReceiverAndReturnType.forEach { argument ->
            val parameter = KotlinParameterStubImpl(
                    parameterList, fqName = null, name = null, isMutable = false, hasValOrVar = false, hasDefaultValue = false
            )
            createTypeReferenceStub(parameter, argument.type(c.typeTable)!!)
        }

        val returnType = typeArgumentList.last().type(c.typeTable)!!
        createTypeReferenceStub(functionType, returnType)
    }

    fun createValueParameterListStub(
            parent: StubElement<out PsiElement>,
            callableProto: MessageLite,
            parameters: List<ProtoBuf.ValueParameter>,
            container: ProtoContainer
    ) {
        val parameterListStub = KotlinPlaceHolderStubImpl<KtParameterList>(parent, KtStubElementTypes.VALUE_PARAMETER_LIST)
        for ((index, valueParameterProto) in parameters.withIndex()) {
            val name = c.nameResolver.getName(valueParameterProto.name)
            val parameterStub = KotlinParameterStubImpl(
                    parameterListStub,
                    name = name.ref(),
                    fqName = null,
                    hasDefaultValue = false,
                    hasValOrVar = false,
                    isMutable = false
            )
            val varargElementType = valueParameterProto.varargElementType(c.typeTable)
            val typeProto = varargElementType ?: valueParameterProto.type(c.typeTable)
            val modifiers = arrayListOf<KtModifierKeywordToken>()

            if (varargElementType != null) { modifiers.add(KtTokens.VARARG_KEYWORD) }
            if (Flags.IS_CROSSINLINE.get(valueParameterProto.flags)) { modifiers.add(KtTokens.CROSSINLINE_KEYWORD) }
            if (Flags.IS_NOINLINE.get(valueParameterProto.flags)) { modifiers.add(KtTokens.NOINLINE_KEYWORD) }
            if (Flags.IS_COROUTINE.get(valueParameterProto.flags)) {
                modifiers.add(KtTokens.COROUTINE_KEYWORD)
            }

            val modifierList = createModifierListStub(parameterStub, modifiers)
            val parameterAnnotations = c.components.annotationLoader.loadValueParameterAnnotations(
                    container, callableProto, callableProto.annotatedCallableKind, index, valueParameterProto
            )
            if (parameterAnnotations.isNotEmpty()) {
                createAnnotationStubs(parameterAnnotations, modifierList ?: createEmptyModifierListStub(parameterStub))
            }

            createTypeReferenceStub(parameterStub, typeProto)
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
        }
        if (typeParameterProto.reified) {
            modifiers.add(KtTokens.REIFIED_KEYWORD)
        }

        val modifierList = createModifierListStub(typeParameterStub, modifiers)

        val annotations = c.components.annotationLoader.loadTypeParameterAnnotations(typeParameterProto, c.nameResolver)
        if (annotations.isNotEmpty()) {
            createAnnotationStubs(
                    annotations,
                    modifierList ?: createEmptyModifierListStub(typeParameterStub))
        }
    }

    private fun Type.isDefaultUpperBound(): Boolean {
        return this.hasClassName() &&
               c.nameResolver.getClassId(className).let { KotlinBuiltIns.FQ_NAMES.any == it.asSingleFqName().toUnsafe() } &&
               this.nullable
    }
}
