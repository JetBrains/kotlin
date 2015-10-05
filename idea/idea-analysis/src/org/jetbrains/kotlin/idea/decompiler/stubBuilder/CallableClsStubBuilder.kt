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

import com.google.protobuf.MessageLite
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.FlagsToModifiers.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.MemberKind
import org.jetbrains.kotlin.serialization.ProtoBuf.Modality
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createCallableStubs(
        parentStub: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        functionProtos: List<ProtoBuf.Function>,
        propertyProtos: List<ProtoBuf.Property>
) {
    for (propertyProto in propertyProtos) {
        if (!shouldSkip(propertyProto.flags, outerContext.nameResolver.getName(propertyProto.name))) {
            CallableClsStubBuilder(parentStub, propertyProto, outerContext, protoContainer).build()
        }
    }
    for (functionProto in functionProtos) {
        if (!shouldSkip(functionProto.flags, outerContext.nameResolver.getName(functionProto.name))) {
            CallableClsStubBuilder(parentStub, functionProto, outerContext, protoContainer).build()
        }
    }
}

fun createConstructorStub(
        parentStub: StubElement<out PsiElement>,
        constructorProto: ProtoBuf.Constructor,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer
) {
    ConstructorClsStubBuilder(parentStub, constructorProto, outerContext, protoContainer).build()
}

private fun shouldSkip(flags: Int, name: Name): Boolean {
    return when (Flags.MEMBER_KIND.get(flags)) {
        MemberKind.FAKE_OVERRIDE, MemberKind.DELEGATION -> true
        //TODO: fix decompiler to use sane criteria
        MemberKind.SYNTHESIZED -> !isComponentLike(name)
        else -> false
    }
}

private class CallableClsStubBuilder private constructor(
        private val parent: StubElement<out PsiElement>,
        private val callableProto: MessageLite,
        outerContext: ClsStubBuilderContext,
        private val protoContainer: ProtoContainer,
        private val callableKind: CallableClsStubBuilder.CallableKind,
        private val nameIndex: Int,
        private val typeParameterList: List<ProtoBuf.TypeParameter>,
        private val valueParameterList: List<ProtoBuf.ValueParameter>,
        private val receiverType: ProtoBuf.Type?,
        private val returnType: ProtoBuf.Type?,
        private val flags: Int
) {
    private val c = outerContext.child(typeParameterList)
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val isTopLevel: Boolean get() = protoContainer.packageFqName != null
    private val callableStub = doCreateCallableStub()

    enum class CallableKind {
        FUN, VAL, VAR
    }

    constructor(
            parent: StubElement<out PsiElement>,
            functionProto: ProtoBuf.Function,
            outerContext: ClsStubBuilderContext,
            protoContainer: ProtoContainer
    ) : this(
            parent, functionProto, outerContext, protoContainer, CallableKind.FUN,
            functionProto.name, functionProto.typeParameterList, functionProto.valueParameterList,
            if (functionProto.hasReceiverType()) functionProto.receiverType else null,
            if (functionProto.hasReturnType()) functionProto.returnType else null,
            functionProto.flags
    )

    constructor(
            parent: StubElement<out PsiElement>,
            propertyProto: ProtoBuf.Property,
            outerContext: ClsStubBuilderContext,
            protoContainer: ProtoContainer
    ) : this(
            parent, propertyProto, outerContext, protoContainer,
            if (Flags.IS_VAR.get(propertyProto.flags)) CallableKind.VAR else CallableKind.VAL,
            propertyProto.name, propertyProto.typeParameterList, emptyList(),
            if (propertyProto.hasReceiverType()) propertyProto.receiverType else null,
            if (propertyProto.hasReturnType()) propertyProto.returnType else null,
            propertyProto.flags
    )

    fun build() {
        createModifierListStub()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameterList)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
    }

    private fun createValueParameterList() {
        if (callableKind == CallableKind.FUN) {
            typeStubBuilder.createValueParameterListStub(callableStub, callableProto, valueParameterList, protoContainer)
        }
    }

    private fun createReceiverTypeReferenceStub() {
        if (receiverType != null) {
            typeStubBuilder.createTypeReferenceStub(callableStub, receiverType)
        }
    }

    private fun createReturnTypeStub() {
        if (returnType != null) {
            typeStubBuilder.createTypeReferenceStub(callableStub, returnType)
        }
    }

    private fun createModifierListStub() {
        val modalityModifiers = if (isTopLevel) listOf() else listOf(MODALITY)
        val constModifiers = if (callableKind == CallableKind.VAL) listOf(CONST) else listOf()

        val additionalModifiers = when (callableKind) {
            CallableKind.FUN -> arrayOf(OPERATOR, INFIX)
            CallableKind.VAL, CallableKind.VAR -> arrayOf(LATEINIT)
            else -> emptyArray<FlagsToModifiers>()
        }

        val relevantModifiers = listOf(VISIBILITY) + constModifiers + modalityModifiers + additionalModifiers
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, flags, relevantModifiers)

        val kind = callableProto.annotatedCallableKind
        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(protoContainer, callableProto, kind)
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    private fun doCreateCallableStub(): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(nameIndex)

        return when (callableKind) {
            CallableKind.FUN -> {
                KotlinFunctionStubImpl(
                        parent,
                        callableName.ref(),
                        isTopLevel,
                        c.containerFqName.child(callableName),
                        isExtension = receiverType != null,
                        hasBlockBody = true,
                        hasBody = Flags.MODALITY.get(flags) != Modality.ABSTRACT,
                        hasTypeParameterListBeforeFunctionName = typeParameterList.isNotEmpty()
                )
            }
            CallableKind.VAL, CallableKind.VAR -> {
                KotlinPropertyStubImpl(
                        parent,
                        callableName.ref(),
                        isVar = callableKind == CallableKind.VAR,
                        isTopLevel = isTopLevel,
                        hasDelegate = false,
                        hasDelegateExpression = false,
                        hasInitializer = false,
                        isExtension = receiverType != null,
                        hasReturnTypeRef = true,
                        fqName = c.containerFqName.child(callableName)
                )
            }
            else -> throw IllegalStateException("Unknown callable kind $callableKind")
        }
    }
}

private class ConstructorClsStubBuilder(
        private val parent: StubElement<out PsiElement>,
        private val constructorProto: ProtoBuf.Constructor,
        outerContext: ClsStubBuilderContext,
        private val protoContainer: ProtoContainer
) {
    private val c = outerContext.child(emptyList())
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val callableStub = doCreateCallableStub()

    fun build() {
        createModifierListStub()
        createValueParameterList()
    }

    private fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, constructorProto, constructorProto.valueParameterList, protoContainer)
    }

    private fun createModifierListStub() {
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, constructorProto.flags, listOf(VISIBILITY))

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer, constructorProto, AnnotatedCallableKind.FUNCTION
        )
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    private fun doCreateCallableStub(): StubElement<out PsiElement> {
        return if (Flags.IS_SECONDARY.get(constructorProto.flags))
            KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.SECONDARY_CONSTRUCTOR)
        else
            KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.PRIMARY_CONSTRUCTOR)
    }
}
