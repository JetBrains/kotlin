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
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.idea.stubindex.KotlinFileStubForIde
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

fun createTopLevelClassStub(
        classId: ClassId,
        classProto: ProtoBuf.Class,
        source: SourceElement?,
        context: ClsStubBuilderContext
): KotlinFileStubImpl {
    val fileStub = createFileStub(classId.packageFqName)
    createClassStub(fileStub, classProto, context.nameResolver, classId, source, context)
    return fileStub
}

fun createPackageFacadeStub(
        packageProto: ProtoBuf.Package,
        packageFqName: FqName,
        c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val fileStub = KotlinFileStubForIde.forFile(packageFqName, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    createCallableStubs(fileStub, c, ProtoContainer.Package(packageFqName, c.nameResolver, c.typeTable, source = null),
                        packageProto.functionList, packageProto.propertyList)
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
    val container = ProtoContainer.Package(
            packageFqName, c.nameResolver, c.typeTable, JvmPackagePartSource(JvmClassName.byClassId(ClassId.topLevel(facadeFqName)), null)
    )
    createCallableStubs(fileStub, c, container, packageProto.functionList, packageProto.propertyList)
    return fileStub
}

fun createMultifileClassStub(
        header: KotlinClassHeader,
        partFiles: List<KotlinJvmBinaryClass>,
        facadeFqName: FqName,
        components: ClsStubBuilderComponents
): KotlinFileStubImpl {
    val packageFqName = facadeFqName.parent()
    val partNames = header.data?.asList()?.map { it.substringAfterLast('/') }
    val fileStub = KotlinFileStubForIde.forMultifileClassStub(facadeFqName, partNames, packageFqName.isRoot)
    setupFileStub(fileStub, packageFqName)
    for (partFile in partFiles) {
        val partHeader = partFile.classHeader
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(partHeader.data!!, partHeader.strings!!)
        val partContext = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
        val container = ProtoContainer.Package(packageFqName, partContext.nameResolver, partContext.typeTable,
                                               JvmPackagePartSource(partFile))
        createCallableStubs(fileStub, partContext, container, packageProto.functionList, packageProto.propertyList)
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
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<KtPackageDirective>(fileStub, KtStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    KotlinPlaceHolderStubImpl<KtImportList>(fileStub, KtStubElementTypes.IMPORT_LIST)
}

fun createStubForPackageName(packageDirectiveStub: KotlinPlaceHolderStubImpl<KtPackageDirective>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments()
    val iterator = segments.listIterator(segments.size)

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>) {
        when (iterator.previousIndex()) {
            -1 -> return
            0 -> {
                KotlinNameReferenceExpressionStubImpl(current, iterator.previous().ref())
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = KotlinPlaceHolderStubImpl<KtDotQualifiedExpression>(current, KtStubElementTypes.DOT_QUALIFIED_EXPRESSION)
                recCreateStubForPackageName(receiver)
                KotlinNameReferenceExpressionStubImpl(receiver, lastSegment.ref())
            }
        }
    }

    recCreateStubForPackageName(packageDirectiveStub)
}

fun createStubForTypeName(
        typeClassId: ClassId,
        parent: StubElement<out PsiElement>,
        onUserTypeLevel: (KotlinUserTypeStub, Int) -> Unit = { x, y -> }
): KotlinUserTypeStub {
    val fqName =
            if (typeClassId.isLocal) KotlinBuiltIns.FQ_NAMES.any
            else typeClassId.asSingleFqName().toUnsafe()
    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): KotlinUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = KotlinUserTypeStubImpl(current)
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        KotlinNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref())
        onUserTypeLevel(userTypeStub, level)
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}

enum class FlagsToModifiers {
    MODALITY {
        override fun getModifiers(flags: Int): KtModifierKeywordToken {
            val modality = Flags.MODALITY.get(flags)
            return when (modality) {
                ProtoBuf.Modality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
                ProtoBuf.Modality.FINAL -> KtTokens.FINAL_KEYWORD
                ProtoBuf.Modality.OPEN -> KtTokens.OPEN_KEYWORD
                ProtoBuf.Modality.SEALED -> KtTokens.SEALED_KEYWORD
                null -> throw IllegalStateException("Unexpected modality: null")
            }
        }
    },

    VISIBILITY {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            val visibility = Flags.VISIBILITY.get(flags)
            return when (visibility) {
                ProtoBuf.Visibility.PRIVATE, ProtoBuf.Visibility.PRIVATE_TO_THIS -> KtTokens.PRIVATE_KEYWORD
                ProtoBuf.Visibility.INTERNAL -> KtTokens.INTERNAL_KEYWORD
                ProtoBuf.Visibility.PROTECTED -> KtTokens.PROTECTED_KEYWORD
                ProtoBuf.Visibility.PUBLIC -> KtTokens.PUBLIC_KEYWORD
                else -> throw IllegalStateException("Unexpected visibility: $visibility")
            }
        }
    },

    INNER {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_INNER.get(flags)) KtTokens.INNER_KEYWORD else null
        }
    },

    CONST {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_CONST.get(flags)) KtTokens.CONST_KEYWORD else null
        }
    },

    LATEINIT {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_LATEINIT.get(flags)) KtTokens.LATEINIT_KEYWORD else null
        }
    },

    OPERATOR {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_OPERATOR.get(flags)) KtTokens.OPERATOR_KEYWORD else null
        }
    },

    INFIX {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_INFIX.get(flags)) KtTokens.INFIX_KEYWORD else null
        }
    },

    DATA {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_DATA.get(flags)) KtTokens.DATA_KEYWORD else null
        }
    },

    EXTERNAL_FUN {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_EXTERNAL_FUNCTION.get(flags)) KtTokens.EXTERNAL_KEYWORD else null
        }
    },

    INLINE {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_INLINE.get(flags)) KtTokens.INLINE_KEYWORD else null
        }
    },

    TAILREC {
        override fun getModifiers(flags: Int): KtModifierKeywordToken? {
            return if (Flags.IS_TAILREC.get(flags)) KtTokens.TAILREC_KEYWORD else null
        }
    };

    abstract fun getModifiers(flags: Int): KtModifierKeywordToken?
}

fun createModifierListStubForDeclaration(
        parent: StubElement<out PsiElement>,
        flags: Int,
        flagsToTranslate: List<FlagsToModifiers> = listOf(),
        additionalModifiers: List<KtModifierKeywordToken> = listOf()
): KotlinModifierListStubImpl {
    assert(flagsToTranslate.isNotEmpty())

    val modifiers = flagsToTranslate.mapNotNull { it.getModifiers(flags) } + additionalModifiers
    return createModifierListStub(parent, modifiers)!!
}

fun createModifierListStub(
        parent: StubElement<out PsiElement>,
        modifiers: Collection<KtModifierKeywordToken>
): KotlinModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }
    return KotlinModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { it in modifiers },
            KtStubElementTypes.MODIFIER_LIST
    )
}

fun createEmptyModifierListStub(parent: KotlinStubBaseImpl<*>): KotlinModifierListStubImpl {
    return KotlinModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { false },
            KtStubElementTypes.MODIFIER_LIST
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
                shortName = annotationClassId.shortClassName.ref(),
                hasValueArguments = false
        )
        if (target != null) {
            KotlinAnnotationUseSiteTargetStubImpl(annotationEntryStubImpl, StringRef.fromString(target.name)!!)
        }
        val constructorCallee = KotlinPlaceHolderStubImpl<KtConstructorCalleeExpression>(annotationEntryStubImpl, KtStubElementTypes.CONSTRUCTOR_CALLEE)
        val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(constructorCallee, KtStubElementTypes.TYPE_REFERENCE)
        createStubForTypeName(annotationClassId, typeReference)
    }
}

val MessageLite.annotatedCallableKind: AnnotatedCallableKind
    get()  {
        return when (this) {
            is ProtoBuf.Property -> AnnotatedCallableKind.PROPERTY
            is ProtoBuf.Function, is ProtoBuf.Constructor -> AnnotatedCallableKind.FUNCTION
            else -> throw IllegalStateException("Unsupported message: $this")
        }
    }

fun Name.ref() = StringRef.fromString(this.asString())!!

fun FqName.ref() = StringRef.fromString(this.asString())!!
