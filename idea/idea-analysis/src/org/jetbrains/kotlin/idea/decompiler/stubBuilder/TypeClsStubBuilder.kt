/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.psi.stubs.impl.KotlinParameterStubImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetNullableType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl
import org.jetbrains.kotlin.psi.JetFunctionType
import org.jetbrains.kotlin.psi.JetFunctionTypeReceiver
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.psi.JetTypeParameterList
import org.jetbrains.kotlin.serialization.ProtoBuf.Type
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl
import org.jetbrains.kotlin.serialization.ProtoBuf.TypeParameter.Variance
import org.jetbrains.kotlin.psi.JetTypeConstraintList
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeConstraintStubImpl
import org.jetbrains.kotlin.name.ClassId
import java.util.ArrayList
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.ModifierMaskUtils
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.serialization.ProtoBuf.Type.Argument.Projection
import org.jetbrains.kotlin.psi.JetProjectionKind

class TypeClsStubBuilder(private val c: ClsStubBuilderContext) {

    fun createTypeReferenceStub(parent: StubElement<out PsiElement>, typeProto: Type) {
        val typeReference = KotlinPlaceHolderStubImpl<JetTypeReference>(parent, JetStubElementTypes.TYPE_REFERENCE)
        createTypeStub(typeReference, typeProto)
    }

    private fun createTypeStub(parent: StubElement<out PsiElement>, type: Type) {
        val isNullable = type.getNullable()
        val effectiveParent = if (isNullable) KotlinPlaceHolderStubImpl<JetNullableType>(parent, JetStubElementTypes.NULLABLE_TYPE) else parent
        when (type.getConstructor().getKind()) {
            Type.Constructor.Kind.CLASS -> {
                createClassReferenceTypeStub(effectiveParent, type)
            }
            Type.Constructor.Kind.TYPE_PARAMETER -> {
                val typeParameterName = c.typeParameters[type.getConstructor().getId()]
                createStubForTypeName(ClassId.topLevel(FqName.topLevel(typeParameterName)), effectiveParent)
            }
        }
    }

    private fun createClassReferenceTypeStub(parent: StubElement<out PsiElement>, type: Type) {
        val classId = c.nameResolver.getClassId(type.getConstructor().getId())
        val fqName = classId.asSingleFqName()
        val isFunctionType = KotlinBuiltIns.isExactFunctionType(fqName)
        val isExtensionFunctionType = KotlinBuiltIns.isExactExtensionFunctionType(fqName)
        if (isFunctionType || isExtensionFunctionType) {
            createFunctionTypeStub(parent, type, isExtensionFunctionType)
            return
        }
        val typeStub = createStubForTypeName(classId, parent)
        val typeArgumentProtoList = type.getArgumentList()
        createTypeArgumentListStub(typeStub, typeArgumentProtoList)
        return
    }

    private fun createTypeArgumentListStub(typeStub: KotlinUserTypeStub, typeArgumentProtoList: List<Type.Argument>) {
        if (typeArgumentProtoList.isEmpty()) {
            return
        }
        val typeArgumentsListStub = KotlinPlaceHolderStubImpl<JetTypeArgumentList>(typeStub, JetStubElementTypes.TYPE_ARGUMENT_LIST)
        typeArgumentProtoList.forEach { typeArgumentProto ->
            val projectionKind = typeArgumentProto.getProjection().toProjectionKind()
            val typeProjection = KotlinTypeProjectionStubImpl(typeArgumentsListStub, projectionKind.ordinal())
            if (projectionKind != JetProjectionKind.STAR) {
                val modifierKeywordToken = projectionKind.getToken() as? JetModifierKeywordToken
                createModifierListStub(typeProjection, modifierKeywordToken.singletonOrEmptyList())
                createTypeReferenceStub(typeProjection, typeArgumentProto.getType())
            }
        }
    }

    private fun Projection.toProjectionKind() = when (this) {
        Projection.IN -> JetProjectionKind.IN
        Projection.OUT -> JetProjectionKind.OUT
        Projection.INV -> JetProjectionKind.NONE
        Projection.STAR -> JetProjectionKind.STAR
    }

    private fun createFunctionTypeStub(parent: StubElement<out PsiElement>, type: Type, isExtensionFunctionType: Boolean) {
        val typeArgumentList = type.getArgumentList()
        val functionType = KotlinPlaceHolderStubImpl<JetFunctionType>(parent, JetStubElementTypes.FUNCTION_TYPE)
        if (isExtensionFunctionType) {
            val functionTypeReceiverStub
                    = KotlinPlaceHolderStubImpl<JetFunctionTypeReceiver>(functionType, JetStubElementTypes.FUNCTION_TYPE_RECEIVER)
            val receiverTypeProto = typeArgumentList.first().getType()
            createTypeReferenceStub(functionTypeReceiverStub, receiverTypeProto)
        }

        val parameterList = KotlinPlaceHolderStubImpl<JetParameterList>(functionType, JetStubElementTypes.VALUE_PARAMETER_LIST)
        val typeArgumentsWithoutReceiverAndReturnType
                = typeArgumentList.subList(if (isExtensionFunctionType) 1 else 0, typeArgumentList.size - 1)
        typeArgumentsWithoutReceiverAndReturnType.forEach { argument ->
            val parameter = KotlinParameterStubImpl(parameterList, fqName = null, name = null, isMutable = false, hasValOrVarNode = false, hasDefaultValue = false)
            createTypeReferenceStub(parameter, argument.getType())
        }

        val returnType = typeArgumentList.last().getType()
        createTypeReferenceStub(functionType, returnType)
    }

    fun createValueParameterListStub(parent: StubElement<out PsiElement>, callableProto: ProtoBuf.Callable, container: ProtoContainer) {
        val callableKind = Flags.CALLABLE_KIND[callableProto.getFlags()]
        if (callableKind == CallableKind.VAL || callableKind == CallableKind.VAR) {
            return
        }
        val parameterListStub = KotlinPlaceHolderStubImpl<JetParameterList>(parent, JetStubElementTypes.VALUE_PARAMETER_LIST)
        for (valueParameterProto in callableProto.getValueParameterList()) {
            val name = c.nameResolver.getName(valueParameterProto.getName())
            val parameterStub = KotlinParameterStubImpl(
                    parameterListStub,
                    name = name.ref(),
                    fqName = null,
                    hasDefaultValue = false,
                    hasValOrVarNode = false,
                    isMutable = false
            )
            val isVararg = valueParameterProto.hasVarargElementType()
            val modifierList = if (isVararg) createModifierListStub(parameterStub, listOf(JetTokens.VARARG_KEYWORD)) else null
            val parameterAnnotations = c.components.annotationLoader.loadValueParameterAnnotations(
                    container, callableProto, c.nameResolver, callableProto.annotatedCallableKind, valueParameterProto
            )
            if (parameterAnnotations.isNotEmpty()) {
                createAnnotationStubs(parameterAnnotations, modifierList ?: createEmptyModifierList(parameterStub))
            }

            val typeProto = if (isVararg) valueParameterProto.getVarargElementType() else valueParameterProto.getType()
            createTypeReferenceStub(parameterStub, typeProto)
        }
    }

    private fun createEmptyModifierList(parameterStub: KotlinParameterStubImpl): KotlinModifierListStubImpl {
        return KotlinModifierListStubImpl(
                parameterStub,
                ModifierMaskUtils.computeMask { false },
                JetStubElementTypes.MODIFIER_LIST
        )
    }

    fun createTypeParameterListStub(
            parent: StubElement<out PsiElement>,
            typeParameterProtoList: List<ProtoBuf.TypeParameter>
    ): List<Pair<Name, Type>> {
        if (typeParameterProtoList.isEmpty()) return listOf()

        val typeParameterListStub = KotlinPlaceHolderStubImpl<JetTypeParameterList>(parent, JetStubElementTypes.TYPE_PARAMETER_LIST)
        val protosForTypeConstraintList = arrayListOf<Pair<Name, Type>>()
        for (proto in typeParameterProtoList) {
            val name = c.nameResolver.getName(proto.getName())
            val typeParameterStub = KotlinTypeParameterStubImpl(
                    typeParameterListStub,
                    name = name.ref(),
                    isInVariance = proto.getVariance() == Variance.IN,
                    isOutVariance = proto.getVariance() == Variance.OUT
            )
            createTypeParameterModifierListStub(typeParameterStub, proto)
            val upperBoundProtos = proto.getUpperBoundList()
            if (upperBoundProtos.isNotEmpty()) {
                val upperBound = upperBoundProtos.first()
                if (!upperBound.isDefaultUpperBound()) {
                    createTypeReferenceStub(typeParameterStub, upperBound)
                }
                protosForTypeConstraintList addAll upperBoundProtos.drop(1).map { Pair(name, it) }
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
        val typeConstraintListStub = KotlinPlaceHolderStubImpl<JetTypeConstraintList>(parent, JetStubElementTypes.TYPE_CONSTRAINT_LIST)
        for ((name, type) in protosForTypeConstraintList) {
            val typeConstraintStub = KotlinTypeConstraintStubImpl(typeConstraintListStub, isDefaultObjectConstraint = false)
            KotlinNameReferenceExpressionStubImpl(typeConstraintStub, name.ref())
            createTypeReferenceStub(typeConstraintStub, type)
        }
    }

    private fun createTypeParameterModifierListStub(
            typeParameterStub: KotlinTypeParameterStubImpl,
            typeParameterProto: ProtoBuf.TypeParameter
    ) {
        val modifiers = ArrayList<JetModifierKeywordToken>()
        when (typeParameterProto.getVariance()) {
            Variance.IN -> modifiers.add(JetTokens.IN_KEYWORD)
            Variance.OUT -> modifiers.add(JetTokens.OUT_KEYWORD)
        }
        if (typeParameterProto.getReified()) {
            modifiers.add(JetTokens.REIFIED_KEYWORD)
        }
        createModifierListStub(typeParameterStub, modifiers)
    }

    private fun Type.isDefaultUpperBound(): Boolean {
        val constructor = getConstructor()
        if (constructor.getKind() != Type.Constructor.Kind.CLASS) {
            return false
        }
        val classId = c.nameResolver.getClassId(constructor.getId())
        return KotlinBuiltIns.isAny(classId.asSingleFqName()) && this.getNullable()
    }
}
