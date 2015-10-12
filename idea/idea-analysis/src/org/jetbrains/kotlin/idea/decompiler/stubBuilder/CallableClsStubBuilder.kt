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
            PropertyClsStubBuilder(parentStub, outerContext, protoContainer, propertyProto).build()
        }
    }
    for (functionProto in functionProtos) {
        if (!shouldSkip(functionProto.flags, outerContext.nameResolver.getName(functionProto.name))) {
            FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionProto).build()
        }
    }
}

fun createConstructorStub(
        parentStub: StubElement<out PsiElement>,
        constructorProto: ProtoBuf.Constructor,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer
) {
    ConstructorClsStubBuilder(parentStub, outerContext, protoContainer, constructorProto).build()
}

private fun shouldSkip(flags: Int, name: Name): Boolean {
    return when (Flags.MEMBER_KIND.get(flags)) {
        MemberKind.FAKE_OVERRIDE, MemberKind.DELEGATION -> true
        //TODO: fix decompiler to use sane criteria
        MemberKind.SYNTHESIZED -> !isComponentLike(name)
        else -> false
    }
}

abstract class CallableClsStubBuilder(
        parent: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protected val protoContainer: ProtoContainer,
        private val typeParameters: List<ProtoBuf.TypeParameter>
) {
    protected val c = outerContext.child(typeParameters)
    protected val typeStubBuilder = TypeClsStubBuilder(c)
    protected val isTopLevel: Boolean get() = protoContainer.packageFqName != null
    protected val callableStub: StubElement<out PsiElement> by lazy(LazyThreadSafetyMode.NONE) { doCreateCallableStub(parent) }

    fun build() {
        createModifierListStub()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameters)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
    }

    abstract val receiverType: ProtoBuf.Type?
    abstract val returnType: ProtoBuf.Type?

    private fun createReceiverTypeReferenceStub() {
        receiverType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it)
        }
    }

    private fun createReturnTypeStub() {
        returnType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it)
        }
    }

    abstract fun createModifierListStub()

    abstract fun createValueParameterList()

    abstract fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement>
}

private class FunctionClsStubBuilder(
        parent: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        private val functionProto: ProtoBuf.Function
) : CallableClsStubBuilder(parent, outerContext, protoContainer, functionProto.typeParameterList) {
    override val receiverType: ProtoBuf.Type?
        get() = if (functionProto.hasReceiverType()) functionProto.receiverType else null

    override val returnType: ProtoBuf.Type?
        get() = if (functionProto.hasReturnType()) functionProto.returnType else null

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, functionProto, functionProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)
        val modifierListStubImpl = createModifierListStubForDeclaration(
                callableStub, functionProto.flags,
                listOf(VISIBILITY, OPERATOR, INFIX) + modalityModifier
        )

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer, functionProto, AnnotatedCallableKind.FUNCTION
        )
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(functionProto.name)

        return KotlinFunctionStubImpl(
                parent,
                callableName.ref(),
                isTopLevel,
                c.containerFqName.child(callableName),
                isExtension = functionProto.hasReceiverType(),
                hasBlockBody = true,
                hasBody = Flags.MODALITY.get(functionProto.flags) != Modality.ABSTRACT,
                hasTypeParameterListBeforeFunctionName = functionProto.typeParameterList.isNotEmpty()
        )
    }
}

private class PropertyClsStubBuilder(
        parent: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        private val propertyProto: ProtoBuf.Property
) : CallableClsStubBuilder(parent, outerContext, protoContainer, propertyProto.typeParameterList) {
    private val isVar = Flags.IS_VAR.get(propertyProto.flags)

    override val receiverType: ProtoBuf.Type?
        get() = if (propertyProto.hasReceiverType()) propertyProto.receiverType else null

    override val returnType: ProtoBuf.Type?
        get() = if (propertyProto.hasReturnType()) propertyProto.returnType else null

    override fun createValueParameterList() {
    }

    override fun createModifierListStub() {
        val constModifier = if (isVar) listOf() else listOf(CONST)
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(
                callableStub, propertyProto.flags,
                listOf(VISIBILITY, LATEINIT) + constModifier + modalityModifier
        )

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY
        )
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(propertyProto.name)

        return KotlinPropertyStubImpl(
                parent,
                callableName.ref(),
                isVar,
                isTopLevel,
                hasDelegate = false,
                hasDelegateExpression = false,
                hasInitializer = false,
                isExtension = propertyProto.hasReceiverType(),
                hasReturnTypeRef = true,
                fqName = c.containerFqName.child(callableName)
        )
    }
}

private class ConstructorClsStubBuilder(
        parent: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        private val constructorProto: ProtoBuf.Constructor
) : CallableClsStubBuilder(parent, outerContext, protoContainer, emptyList()) {
    override val receiverType: ProtoBuf.Type?
        get() = null

    override val returnType: ProtoBuf.Type?
        get() = null

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, constructorProto, constructorProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, constructorProto.flags, listOf(VISIBILITY))

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer, constructorProto, AnnotatedCallableKind.FUNCTION
        )
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        return if (Flags.IS_SECONDARY.get(constructorProto.flags))
            KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.SECONDARY_CONSTRUCTOR)
        else
            KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.PRIMARY_CONSTRUCTOR)
    }
}
