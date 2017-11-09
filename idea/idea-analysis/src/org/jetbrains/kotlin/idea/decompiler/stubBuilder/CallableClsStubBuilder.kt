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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.flags.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.MemberKind
import org.jetbrains.kotlin.serialization.ProtoBuf.Modality
import org.jetbrains.kotlin.serialization.deserialization.*

fun createDeclarationsStubs(
        parentStub: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        packageProto: ProtoBuf.Package
) {
    createDeclarationsStubs(
            parentStub, outerContext, protoContainer, packageProto.functionList, packageProto.propertyList, packageProto.typeAliasList)
}

fun createDeclarationsStubs(
        parentStub: StubElement<out PsiElement>,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer,
        functionProtos: List<ProtoBuf.Function>,
        propertyProtos: List<ProtoBuf.Property>,
        typeAliasesProtos: List<ProtoBuf.TypeAlias>
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

    for (typeAliasProto in typeAliasesProtos) {
        createTypeAliasStub(parentStub, typeAliasProto, protoContainer, outerContext)
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
        MemberKind.SYNTHESIZED -> !DataClassDescriptorResolver.isComponentLike(name)
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
    protected val isTopLevel: Boolean get() = protoContainer is ProtoContainer.Package
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
    abstract val receiverAnnotations: List<ClassIdWithTarget>

    abstract val returnType: ProtoBuf.Type?

    private fun createReceiverTypeReferenceStub() {
        receiverType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it, this::receiverAnnotations)
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
        get() = functionProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() {
            return c.components.annotationLoader
                    .loadExtensionReceiverParameterAnnotations(protoContainer, functionProto, AnnotatedCallableKind.FUNCTION)
                    .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
        }

    override val returnType: ProtoBuf.Type?
        get() = functionProto.returnType(c.typeTable)

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, functionProto, functionProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)
        val modifierListStubImpl = createModifierListStubForDeclaration(
                callableStub, functionProto.flags,
                listOf(VISIBILITY, OPERATOR, INFIX, EXTERNAL_FUN, INLINE, TAILREC, SUSPEND) + modalityModifier
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
                isExtension = functionProto.hasReceiver(),
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
        get() = propertyProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() {
            return c.components.annotationLoader
                    .loadExtensionReceiverParameterAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY_GETTER)
                    .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
        }

    override val returnType: ProtoBuf.Type?
        get() = propertyProto.returnType(c.typeTable)

    override fun createValueParameterList() {
    }

    override fun createModifierListStub() {
        val constModifier = if (isVar) listOf() else listOf(CONST)
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(
                callableStub, propertyProto.flags,
                listOf(VISIBILITY, LATEINIT, EXTERNAL_PROPERTY) + constModifier + modalityModifier
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
                isExtension = propertyProto.hasReceiver(),
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

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() = emptyList()

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
            KotlinPlaceHolderStubImpl(parent, KtStubElementTypes.SECONDARY_CONSTRUCTOR)
        else
            KotlinPlaceHolderStubImpl(parent, KtStubElementTypes.PRIMARY_CONSTRUCTOR)
    }
}
