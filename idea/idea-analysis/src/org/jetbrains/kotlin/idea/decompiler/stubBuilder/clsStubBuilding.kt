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
import org.jetbrains.kotlin.idea.stubindex.KotlinFileStubForIde
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

fun createTopLevelClassStub(classId: ClassId, classProto: ProtoBuf.Class, context: ClsStubBuilderContext): KotlinFileStubImpl {
    val fileStub = createFileStub(classId.getPackageFqName())
    createClassStub(fileStub, classProto, classId, context)
    return fileStub
}

fun createPackageFacadeStub(
        packageProto: ProtoBuf.Package,
        packageFqName: FqName,
        c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val fileStub = KotlinFileStubForIde.forFile(packageFqName, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    val container = ProtoContainer(null, packageFqName)
    for (callableProto in packageProto.getMemberList()) {
        createCallableStub(fileStub, callableProto, c, container)
    }
    return fileStub
}

fun createFileFacadeStub(
        packageProto: ProtoBuf.Package,
        facadeFqName: FqName,
        c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val packageFqName = facadeFqName.parent()
    val fileStub = KotlinFileStubForIde.forFileFacadeStub(facadeFqName, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    val container = ProtoContainer(null, facadeFqName.parent())
    for (callableProto in packageProto.getMemberList()) {
        createCallableStub(fileStub, callableProto, c, container)
    }
    return fileStub
}

fun createMultifileClassStub(
        multifileClass: KotlinJvmBinaryClass,
        partFiles: List<KotlinJvmBinaryClass>,
        facadeFqName: FqName,
        components: ClsStubBuilderComponents
): KotlinFileStubImpl {
    val packageFqName = facadeFqName.parent()
    val partNames = multifileClass.classHeader.filePartClassNames?.asList()
    val fileStub = KotlinFileStubForIde.forMultifileClassStub(facadeFqName, partNames, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    val multifileClassContainer = ProtoContainer(null, packageFqName)
    for (partFile in partFiles) {
        val partHeader = partFile.classHeader
        val partData = JvmProtoBufUtil.readPackageDataFrom(partHeader.annotationData!!, partHeader.strings!!)
        val partContext = components.createContext(partData.nameResolver, packageFqName)
        for (partMember in partData.packageProto.memberList) {
            createCallableStub(fileStub, partMember, partContext, multifileClassContainer)
        }
    }
    return fileStub
}

fun createIncompatibleAbiVersionFileStub() = createFileStub(FqName.ROOT)

fun createFileStub(packageFqName: FqName): KotlinFileStubImpl {
    val fileStub = KotlinFileStubForIde.forFile(packageFqName, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    return fileStub
}

private fun setupFileStub(fileStub: KotlinFileStubImpl, packageFqName: FqName) {
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<JetPackageDirective>(fileStub, JetStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    KotlinPlaceHolderStubImpl<JetImportList>(fileStub, JetStubElementTypes.IMPORT_LIST)
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
                ProtoBuf.Modality.SEALED -> JetTokens.SEALED_KEYWORD
                null -> throw IllegalStateException("Unexpected modality: null")
            }
        }
    },

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
    },

    INNER {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            return if (Flags.INNER.get(flags)) JetTokens.INNER_KEYWORD else null
        }
    },

    CONST {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            return if (Flags.IS_CONST.get(flags)) JetTokens.CONST_KEYWORD else null
        }
    },

    LATEINIT {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            return if (Flags.LATE_INIT.get(flags)) JetTokens.LATE_INIT_KEYWORD else null
        }
    },

    OPERATOR {
        override fun getModifiers(flags: Int): JetModifierKeywordToken? {
            return if (Flags.IS_OPERATOR.get(flags)) JetTokens.OPERATOR_KEYWORD else null
        }
    };

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

fun createAnnotationStubs(annotationIds: List<ClassId>, parent: KotlinStubBaseImpl<*>) {
    return createTargetedAnnotationStubs(annotationIds.map { ClassIdWithTarget(it, null) }, parent)
}

fun createTargetedAnnotationStubs(
        annotationIds: List<ClassIdWithTarget>,
        parent: KotlinStubBaseImpl<*>
) {
    if (annotationIds.isEmpty()) return

    annotationIds.forEach { annotation ->
        val (annotationClassId, target) = annotation
        val annotationEntryStubImpl = KotlinAnnotationEntryStubImpl(
                parent,
                shortName = annotationClassId.getShortClassName().ref(),
                hasValueArguments = false
        )
        if (target != null) {
            KotlinAnnotationUseSiteTargetStubImpl(annotationEntryStubImpl, StringRef.fromString(target.name())!!)
        }
        val constructorCallee = KotlinPlaceHolderStubImpl<JetConstructorCalleeExpression>(annotationEntryStubImpl, JetStubElementTypes.CONSTRUCTOR_CALLEE)
        val typeReference = KotlinPlaceHolderStubImpl<JetTypeReference>(constructorCallee, JetStubElementTypes.TYPE_REFERENCE)
        createStubForTypeName(annotationClassId, typeReference)
    }
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

fun Name.ref() = StringRef.fromString(this.asString())!!

fun FqName.ref() = StringRef.fromString(this.asString())!!
