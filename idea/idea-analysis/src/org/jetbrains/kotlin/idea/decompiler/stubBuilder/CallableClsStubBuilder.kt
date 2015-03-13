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
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.FlagsToModifiers.MODALITY
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.FlagsToModifiers.VISIBILITY
import org.jetbrains.kotlin.psi.JetSecondaryConstructor
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.MemberKind
import org.jetbrains.kotlin.serialization.ProtoBuf.Modality
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createCallableStub(
        parentStub: StubElement<out PsiElement>,
        callableProto: ProtoBuf.Callable,
        outerContext: ClsStubBuilderContext,
        protoContainer: ProtoContainer
) {
    if (!shouldSkip(callableProto, outerContext.nameResolver)) {
        CallableClsStubBuilder(parentStub, callableProto, outerContext, protoContainer).build()
    }
}

private fun shouldSkip(callableProto: ProtoBuf.Callable, nameResolver: NameResolver): Boolean {
    val memberKind = Flags.MEMBER_KIND[callableProto.getFlags()]
    return when (memberKind) {
        MemberKind.FAKE_OVERRIDE, MemberKind.DELEGATION -> true
        //TODO: fix decompiler to use sane criteria
        MemberKind.SYNTHESIZED -> !isComponentLike(nameResolver.getName(callableProto.getName()))
        else -> false
    }
}

private class CallableClsStubBuilder(
        private val parent: StubElement<out PsiElement>,
        private val callableProto: ProtoBuf.Callable,
        outerContext: ClsStubBuilderContext,
        private val protoContainer: ProtoContainer
) {
    private val c = outerContext.child(callableProto.getTypeParameterList())
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val isTopLevel: Boolean get() = protoContainer.packageFqName != null
    private val callableKind = Flags.CALLABLE_KIND[callableProto.getFlags()]
    private val isConstructor = callableKind == ProtoBuf.Callable.CallableKind.CONSTRUCTOR
    private val callableStub = doCreateCallableStub()

    fun build() {
        createModifierListStub()
        val typeParameterList = if (isConstructor) emptyList() else callableProto.getTypeParameterList()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameterList)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
    }

    private fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, callableProto, protoContainer)
    }

    private fun createReceiverTypeReferenceStub() {
        if (callableProto.hasReceiverType()) {
            typeStubBuilder.createTypeReferenceStub(callableStub, callableProto.getReceiverType())
        }
    }

    private fun createReturnTypeStub() {
        if (!isConstructor)
            typeStubBuilder.createTypeReferenceStub(callableStub, callableProto.getReturnType())
    }

    private fun createModifierListStub() {
        val isModalityIrrelevant = isTopLevel || isConstructor
        val relevantModifiers = if (isModalityIrrelevant) listOf(VISIBILITY) else listOf(VISIBILITY, MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, callableProto.getFlags(), relevantModifiers)
        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer, callableProto, c.nameResolver, callableProto.annotatedCallableKind
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    private fun doCreateCallableStub(): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(callableProto.getName())

        return when (callableKind) {
            ProtoBuf.Callable.CallableKind.FUN -> {
                KotlinFunctionStubImpl(
                        parent,
                        callableName.ref(),
                        isTopLevel,
                        c.containerFqName.child(callableName),
                        isExtension = callableProto.hasReceiverType(),
                        hasBlockBody = true,
                        hasBody = Flags.MODALITY[callableProto.getFlags()] != Modality.ABSTRACT,
                        hasTypeParameterListBeforeFunctionName = callableProto.getTypeParameterList().isNotEmpty(),
                        isProbablyNothingType = isProbablyNothing(callableProto)
                )
            }
            ProtoBuf.Callable.CallableKind.VAL, ProtoBuf.Callable.CallableKind.VAR -> {
                KotlinPropertyStubImpl(
                        parent,
                        callableName.ref(),
                        isVar = callableKind == CallableKind.VAR,
                        isTopLevel = isTopLevel,
                        hasDelegate = false,
                        hasDelegateExpression = false,
                        hasInitializer = false,
                        hasReceiverTypeRef = callableProto.hasReceiverType(),
                        hasReturnTypeRef = true,
                        fqName = c.containerFqName.child(callableName),
                        isProbablyNothingType = isProbablyNothing(callableProto)
                )
            }
            ProtoBuf.Callable.CallableKind.CONSTRUCTOR -> {
                KotlinPlaceHolderStubImpl<JetSecondaryConstructor>(parent, JetStubElementTypes.SECONDARY_CONSTRUCTOR)
            }
            else -> throw IllegalStateException("Unknown callable kind $callableKind")
        }
    }

    //TODO: remove isProbablyNothing from stubs
    private fun isProbablyNothing(callableProto: ProtoBuf.Callable): Boolean {
        val constructor = callableProto.getReturnType().getConstructor()
        return constructor.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS &&
               c.nameResolver.getClassId(constructor.getId()).getRelativeClassName().shortName().asString() == "Nothing"
    }
}
