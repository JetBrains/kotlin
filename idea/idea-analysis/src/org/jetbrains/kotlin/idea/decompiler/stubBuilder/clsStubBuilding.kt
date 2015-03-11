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
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetConstructorCalleeExpression
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createTopLevelClassStub(classId: ClassId, classProto: ProtoBuf.Class, context: ClsStubBuilderContext): KotlinFileStubImpl {
    val fileStub = createFileStub(classId.getPackageFqName())
    createClassStub(fileStub, classProto, classId, context)
    return fileStub
}

fun createPackageFacadeFileStub(
        packageProto: ProtoBuf.Package,
        packageFqName: FqName,
        c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val fileStub = createFileStub(packageFqName)
    val container = ProtoContainer(null, packageFqName)
    for (callableProto in packageProto.getMemberList()) {
        createCallableStub(fileStub, callableProto, c, container)
    }
    return fileStub
}

fun createIncompatibleAbiVersionFileStub() = createFileStub(FqName.ROOT)

fun createFileStub(packageFqName: FqName): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl(null, packageFqName.asString(), packageFqName.isRoot())
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<JetPackageDirective>(fileStub, JetStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    return fileStub
}

fun createStubForPackageName(packageDirectiveStub: KotlinPlaceHolderStubImpl<JetPackageDirective>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments().toArrayList()
    val iterator = segments.listIterator(segments.size())

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>) {
        when (iterator.previousIndex()) {
            -1 -> return
            0 -> {
                KotlinNameReferenceExpressionStubImpl(current, iterator.previous().ref())
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = KotlinPlaceHolderStubImpl<JetDotQualifiedExpression>(current, JetStubElementTypes.DOT_QUALIFIED_EXPRESSION)
                recCreateStubForPackageName(receiver)
                KotlinNameReferenceExpressionStubImpl(receiver, lastSegment.ref())
            }
        }
    }

    recCreateStubForPackageName(packageDirectiveStub)
}

fun createStubForTypeName(typeClassId: ClassId, parent: StubElement<out PsiElement>): KotlinUserTypeStub {
    val fqName =
            if (typeClassId.isLocal()) KotlinBuiltIns.FQ_NAMES.any
            else typeClassId.asSingleFqName()
    val segments = fqName.pathSegments().toArrayList()
    assert(segments.isNotEmpty())
    val iterator = segments.listIterator(segments.size())

    fun recCreateStubForType(current: StubElement<out PsiElement>): KotlinUserTypeStub {
        val lastSegment = iterator.previous()
        val userTypeStub = KotlinUserTypeStubImpl(current, isAbsoluteInRootPackage = false)
        if (iterator.hasPrevious()) {
            recCreateStubForType(userTypeStub)
        }
        KotlinNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref())
        return userTypeStub
    }

    return recCreateStubForType(parent)
}

enum class FlagsToModifiers {
    MODALITY {
        override fun getModifiers(flags: Int): JetModifierKeywordToken {
            val modality = Flags.MODALITY.get(flags)
            return when (modality) {
                ProtoBuf.Modality.ABSTRACT -> JetTokens.ABSTRACT_KEYWORD
                ProtoBuf.Modality.FINAL -> JetTokens.FINAL_KEYWORD
                ProtoBuf.Modality.OPEN -> JetTokens.OPEN_KEYWORD
                else -> throw IllegalStateException("Unexpected modality: $modality")
            }
        }
    }

    VISIBILITY {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            val visibility = Flags.VISIBILITY.get(flags)
            return when (visibility) {
                ProtoBuf.Visibility.PRIVATE, ProtoBuf.Visibility.PRIVATE_TO_THIS -> JetTokens.PRIVATE_KEYWORD
                ProtoBuf.Visibility.INTERNAL -> JetTokens.INTERNAL_KEYWORD
                ProtoBuf.Visibility.PROTECTED -> JetTokens.PROTECTED_KEYWORD
                ProtoBuf.Visibility.PUBLIC -> JetTokens.PUBLIC_KEYWORD
                else -> throw IllegalStateException("Unexpected visibility: $visibility")
            }
        }
    }

    INNER {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            return if (Flags.INNER.get(flags)) JetTokens.INNER_KEYWORD else null
        }
    }

    abstract fun getModifiers(flags: Int): JetModifierKeywordToken?
}

fun createModifierListStubForDeclaration(
        parent: StubElement<out PsiElement>,
        flags: Int,
        flagsToTranslate: List<FlagsToModifiers> = listOf(),
        additionalModifiers: List<JetModifierKeywordToken> = listOf()
): KotlinModifierListStubImpl {
    assert(flagsToTranslate.isNotEmpty())

    val modifiers = flagsToTranslate.map { it.getModifiers(flags) }.filterNotNull() + additionalModifiers
    return createModifierListStub(parent, modifiers)!!
}

fun createModifierListStub(
        parent: StubElement<out PsiElement>,
        modifiers: Collection<JetModifierKeywordToken>
): KotlinModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }
    return KotlinModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { it in modifiers },
            JetStubElementTypes.MODIFIER_LIST
    )
}

fun createAnnotationStubs(annotationIds: List<ClassId>, modifierList: KotlinModifierListStubImpl) = annotationIds.forEach {
    annotationClassId ->
    val annotationEntryStubImpl = KotlinAnnotationEntryStubImpl(
            modifierList,
            shortName = annotationClassId.asSingleFqName().shortName().ref(),
            hasValueArguments = false
    )
    val constructorCallee = KotlinPlaceHolderStubImpl<JetConstructorCalleeExpression>(annotationEntryStubImpl, JetStubElementTypes.CONSTRUCTOR_CALLEE)
    val typeReference = KotlinPlaceHolderStubImpl<JetTypeReference>(constructorCallee, JetStubElementTypes.TYPE_REFERENCE)
    createStubForTypeName(annotationClassId, typeReference)
}

val ProtoBuf.Callable.annotatedCallableKind: AnnotatedCallableKind
    get()  {
        val callableKind = Flags.CALLABLE_KIND[getFlags()]
        return when (callableKind) {
            CallableKind.VAL, CallableKind.VAR -> AnnotatedCallableKind.PROPERTY
            CallableKind.FUN, CallableKind.CONSTRUCTOR -> AnnotatedCallableKind.FUNCTION
            else -> throw IllegalStateException("Unsupported callable kind: ${callableKind}")
        }
    }

fun Name.ref() = StringRef.fromString(this.asString())

fun FqName.ref() = StringRef.fromString(this.asString())
