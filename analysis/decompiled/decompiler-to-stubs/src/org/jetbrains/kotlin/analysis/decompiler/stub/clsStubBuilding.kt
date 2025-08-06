/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.FlagsToModifiers
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createTopLevelClassStub(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    source: SourceElement?,
    context: ClsStubBuilderContext,
    isScript: Boolean,
): KotlinFileStubImpl {
    val fileStub = createFileStub(classId.packageFqName, isScript)
    createClassStub(fileStub, classProto, context.nameResolver, classId, source, context)
    return fileStub
}

fun createPackageFacadeStub(
    packageProto: ProtoBuf.Package,
    packageFqName: FqName,
    c: ClsStubBuilderContext,
): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl.forFile(packageFqName)
    setupFileStub(fileStub)
    createPackageDeclarationsStubs(
        fileStub, c, ProtoContainer.Package(packageFqName, c.nameResolver, c.typeTable, source = null), packageProto
    )
    return fileStub
}

fun createFileFacadeStub(
    packageFqName: FqName,
    packageProto: ProtoBuf.Package,
    facadeFqName: FqName,
    jvmFqName: FqName,
    c: ClsStubBuilderContext,
): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl.forFacade(
        packageFqName = packageFqName,
        facadeFqName = jvmFqName,
    )

    setupFileStub(fileStub)
    val container = ProtoContainer.Package(
        packageFqName, c.nameResolver, c.typeTable,
        JvmPackagePartSource(
            className = JvmClassName.byClassId(ClassId.topLevel(facadeFqName)),
            facadeClassName = null,
            jvmClassName = JvmClassName.byClassId(ClassId.topLevel(jvmFqName)),
            packageProto,
            c.nameResolver
        )
    )

    createPackageDeclarationsStubs(fileStub, c, container, packageProto)
    return fileStub
}

fun createMultifileClassStub(
    packageFqName: FqName,
    partFiles: List<KotlinJvmBinaryClass>,
    jvmFqName: FqName,
    components: ClsStubBuilderComponents,
): KotlinFileStubImpl {
    val partNames = partFiles.map { it.classId.shortClassName.asString() }
    val fileStub = KotlinFileStubImpl.forMultifileClass(
        packageFqName = packageFqName,
        facadeFqName = jvmFqName,
        partNames = partNames,
    )

    setupFileStub(fileStub)
    for (partFile in partFiles) {
        val partHeader = partFile.classHeader
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(partHeader.data!!, partHeader.strings!!)
        val partContext = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
        val container = ProtoContainer.Package(
            packageFqName, partContext.nameResolver, partContext.typeTable,
            JvmPackagePartSource(partFile, packageProto, nameResolver)
        )
        createPackageDeclarationsStubs(fileStub, partContext, container, packageProto)
    }
    return fileStub
}

fun createIncompatibleAbiVersionFileStub(errorMessage: String): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl.forInvalid(errorMessage)
    setupFileStub(fileStub)
    return fileStub
}

fun createFileStub(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl {
    val fileStub = if (isScript) {
        KotlinFileStubImpl.forScript(packageFqName)
    } else {
        KotlinFileStubImpl.forFile(packageFqName)
    }

    setupFileStub(fileStub)
    return fileStub
}

private fun setupFileStub(fileStub: KotlinFileStubImpl) {
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<KtPackageDirective>(fileStub, KtStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, fileStub.getPackageFqName())
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

/**
 * @param abbreviatedType The abbreviated type of the expanded type [typeClassId], if applicable. The abbreviated type always applies to the
 *  innermost type. For example, if we have `typealias Alias = Map.Entry`, `Alias` refers to `Entry` but not `Map`. It would be correct to
 *  refer back from `Entry` to `Alias`, but not from `Map` to `Alias`.
 *
 *  Furthermore, an outer type in an already expanded type (e.g. `Map` in `Map.Entry`) cannot have originated from a type alias, because
 *  nested types cannot be accessed from type aliases. (Although this would change with KT-34281.)
 */
fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    abbreviatedType: KotlinClassTypeBean? = null,
    upperBoundFun: ((Int) -> KotlinTypeBean?)? = null,
    bindTypeArguments: (KotlinUserTypeStub, Int) -> Unit = { _, _ -> },
): KotlinUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.any
    else typeClassId.asSingleFqName().toUnsafe()

    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())
    val classesNestedLevel = segments.size - if (substituteWithAny) 1 else typeClassId.packageFqName.pathSegments().size

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): KotlinUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = KotlinUserTypeStubImpl(current, upperBoundFun?.invoke(level), abbreviatedType.takeIf { level == 0 })
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        KotlinNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref(), level < classesNestedLevel)
        if (!substituteWithAny) {
            bindTypeArguments(userTypeStub, level)
        }
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}

fun createModifierListStubForDeclaration(
    parent: StubElement<out PsiElement>,
    flags: Int,
    flagsToTranslate: List<FlagsToModifiers>,
    additionalModifiers: List<KtModifierKeywordToken>,
    returnValueStatus: Flags.FlagField<ProtoBuf.ReturnValueStatus>?,
): KotlinModifierListStubImpl {
    assert(flagsToTranslate.isNotEmpty())

    val modifiers = flagsToTranslate.mapNotNull { it.getModifiers(flags) } + additionalModifiers
    return createModifierListStub(
        parent,
        modifiers,
        returnValueStatus?.get(flags) ?: ProtoBuf.ReturnValueStatus.UNSPECIFIED,
    )!!
}

fun createModifierListStub(
    parent: StubElement<out PsiElement>,
    modifiers: Collection<KtModifierKeywordToken>,
    returnValueStatus: ProtoBuf.ReturnValueStatus,
): KotlinModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }

    val regularMask = ModifierMaskUtils.computeMask { it in modifiers }

    @OptIn(KtImplementationDetail::class)
    val specialMask = ModifierMaskUtils.computeMaskForSpecialFlags { flag ->
        when (flag) {
            KotlinModifierListStub.SpecialFlag.MustUseReturnValue   -> returnValueStatus == ProtoBuf.ReturnValueStatus.MUST_USE
            KotlinModifierListStub.SpecialFlag.IgnorableReturnValue -> returnValueStatus == ProtoBuf.ReturnValueStatus.EXPLICITLY_IGNORABLE
        }
    }

    return KotlinModifierListStubImpl(
        parent,
        regularMask or specialMask,
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

fun createAnnotationStubs(annotations: List<AnnotationWithArgs>, parent: KotlinStubBaseImpl<*>) {
    return createTargetedAnnotationStubs(annotations.map { AnnotationWithTarget(it, null) }, parent)
}

fun createTargetedAnnotationStubs(
    annotations: List<AnnotationWithTarget>,
    parent: KotlinStubBaseImpl<*>,
) {
    if (annotations.isEmpty()) return

    annotations.forEach { annotation ->
        val (annotationWithArgs, target) = annotation
        val annotationEntryStubImpl = KotlinAnnotationEntryStubImpl(
            parent,
            _shortName = annotationWithArgs.classId.shortClassName.ref(),
            hasValueArguments = false,
            annotationWithArgs.args,
        )
        if (target != null) {
            KotlinAnnotationUseSiteTargetStubImpl(annotationEntryStubImpl, StringRef.fromString(target.name)!!)
        }
        val constructorCallee =
            KotlinPlaceHolderStubImpl<KtConstructorCalleeExpression>(annotationEntryStubImpl, KtStubElementTypes.CONSTRUCTOR_CALLEE)
        val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(constructorCallee, KtStubElementTypes.TYPE_REFERENCE)
        createStubForTypeName(annotationWithArgs.classId, typeReference)
    }
}

internal fun createStubOrigin(protoContainer: ProtoContainer): KotlinStubOrigin? {
    if (protoContainer is ProtoContainer.Package) {
        val source = protoContainer.source
        if (source is FacadeClassSource) {
            val className = source.className.internalName
            val facadeClassName = source.facadeClassName?.internalName
            if (facadeClassName != null) {
                return KotlinStubOrigin.MultiFileFacade(className, facadeClassName)
            }
            val jvmClassName = if (source is JvmPackagePartSource) source.jvmClassName?.internalName else null
            return KotlinStubOrigin.Facade(className = className, jvmClassName = if (jvmClassName != className) jvmClassName else null)
        }
    }

    return null
}

val MessageLite.annotatedCallableKind: AnnotatedCallableKind
    get() = when (this) {
        is ProtoBuf.Property -> AnnotatedCallableKind.PROPERTY
        is ProtoBuf.Function, is ProtoBuf.Constructor -> AnnotatedCallableKind.FUNCTION
        else -> throw IllegalStateException("Unsupported message: $this")
    }

fun Name.ref() = StringRef.fromString(this.asString())!!

fun FqName.ref() = StringRef.fromString(this.asString())!!

fun computeParameterName(name: Name): Name {
    return when {
        name == SpecialNames.IMPLICIT_SET_PARAMETER -> StandardNames.DEFAULT_VALUE_PARAMETER
        SpecialNames.isAnonymousParameterName(name) -> Name.identifier("_")
        else -> name
    }
}
