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
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.CallableKind
import org.jetbrains.kotlin.serialization.ProtoBuf.MemberKind
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
    private val isConstructor = callableKind == CallableKind.CONSTRUCTOR
    private val isPrimaryConstructor = isConstructor && parent is KotlinClassStubImpl
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
        val modalityModifiers = if (isModalityIrrelevant) listOf() else listOf(MODALITY)
        val constModifiers = if (callableKind == CallableKind.VAL) listOf(CONST) else listOf()

        val additionalModifiers = when (callableKind) {
            CallableKind.FUN -> arrayOf(OPERATOR, INFIX)
            CallableKind.VAL, CallableKind.VAR -> arrayOf(LATEINIT)
            else -> emptyArray<FlagsToModifiers>()
        }

        val relevantModifiers = listOf(VISIBILITY) + constModifiers + modalityModifiers + additionalModifiers
        val modifierListStubImpl = createModifierListStubForDeclaration(
                callableStub, callableProto.getFlags(), relevantModifiers
        )

        val kind = callableProto.annotatedCallableKind
        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(protoContainer, callableProto, c.nameResolver, kind)
        createTargetedAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    private fun doCreateCallableStub(): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(callableProto.getName())

        return when (callableKind) {
            CallableKind.FUN -> {
                KotlinFunctionStubImpl(
                        parent,
                        callableName.ref(),
                        isTopLevel,
                        c.containerFqName.child(callableName),
                        isExtension = callableProto.hasReceiverType(),
                        hasBlockBody = true,
                        hasBody = Flags.MODALITY[callableProto.getFlags()] != Modality.ABSTRACT,
                        hasTypeParameterListBeforeFunctionName = callableProto.getTypeParameterList().isNotEmpty()
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
                        isExtension = callableProto.hasReceiverType(),
                        hasReturnTypeRef = true,
                        fqName = c.containerFqName.child(callableName)
                )
            }
            CallableKind.CONSTRUCTOR -> {
                if (isPrimaryConstructor)
                    KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.PRIMARY_CONSTRUCTOR)
                else
                    KotlinPlaceHolderStubImpl(parent, JetStubElementTypes.SECONDARY_CONSTRUCTOR)
            }
            else -> throw IllegalStateException("Unknown callable kind $callableKind")
        }
    }
}
