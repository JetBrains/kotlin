// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.FlagsToModifiers
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createTopLevelClassStub(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    source: SourceElement?,
    context: ClsStubBuilderContext,
    isScript: Boolean
): KotlinFileStubImpl {
    val fileStub = createFileStub(classId.packageFqName, isScript)
    createClassStub(fileStub, classProto, context.nameResolver, classId, source, context)
    return fileStub
}

fun createPackageFacadeStub(
    packageProto: ProtoBuf.Package,
    packageFqName: FqName,
    c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl.forFile(packageFqName, isScript = false)
    setupFileStub(fileStub, packageFqName)
    createPackageDeclarationsStubs(
        fileStub, c, ProtoContainer.Package(packageFqName, c.nameResolver, c.typeTable, source = null), packageProto
    )
    return fileStub
}

fun createFileFacadeStub(
    packageProto: ProtoBuf.Package,
    facadeFqName: FqName,
    c: ClsStubBuilderContext
): KotlinFileStubImpl {
    val packageFqName = facadeFqName.parent()
    val fileStub = KotlinFileStubImpl.forFileFacadeStub(facadeFqName)
    setupFileStub(fileStub, packageFqName)
    val container = ProtoContainer.Package(
        packageFqName, c.nameResolver, c.typeTable,
        JvmPackagePartSource(JvmClassName.byClassId(ClassId.topLevel(facadeFqName)), null, packageProto, c.nameResolver)
    )
    createPackageDeclarationsStubs(fileStub, c, container, packageProto)
    return fileStub
}

fun createMultifileClassStub(
    header: KotlinClassHeader,
    partFiles: List<KotlinJvmBinaryClass>,
    facadeFqName: FqName,
    components: ClsStubBuilderComponents
): KotlinFileStubImpl {
    val packageFqName = header.packageName?.let { FqName(it) } ?: facadeFqName.parent()
    val partNames = header.data?.asList()?.map { it.substringAfterLast('/') }
    val fileStub = KotlinFileStubImpl.forMultifileClassStub(packageFqName, facadeFqName, partNames)
    setupFileStub(fileStub, packageFqName)
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

fun createIncompatibleAbiVersionFileStub() = createFileStub(FqName.ROOT, isScript = false)

fun createFileStub(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl.forFile(packageFqName, isScript)
    setupFileStub(fileStub, packageFqName)
    return fileStub
}

private fun setupFileStub(fileStub: KotlinFileStubImpl, packageFqName: FqName) {
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<KtPackageDirective>(fileStub, KtStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName, 0)
    KotlinPlaceHolderStubImpl<KtImportList>(fileStub, KtStubElementTypes.IMPORT_LIST)
}

fun createStubForPackageName(stub: StubElement<*>, packageFqName: FqName, classLength: Int){
    val segments = packageFqName.pathSegments()
    val iterator = segments.listIterator(segments.size)
    val packageLength = segments.size - classLength

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>){
        when (val index = iterator.previousIndex()) {
            -1 -> return
            0 -> {
                KotlinNameReferenceExpressionStubImpl(current, iterator.previous().ref(), false)
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = KotlinPlaceHolderStubImpl<KtDotQualifiedExpression>(current, KtStubElementTypes.DOT_QUALIFIED_EXPRESSION)
                recCreateStubForPackageName(receiver)
                KotlinNameReferenceExpressionStubImpl(receiver, lastSegment.ref(), index >= packageLength)
            }
        }
    }

    recCreateStubForPackageName(stub)
}

fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    bindTypeArguments: (KotlinUserTypeStub, Int) -> Unit = { _, _ -> }
): KotlinUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.any
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

fun createAnnotationStubs(annotations: List<AnnotationWithArgs>, parent: KotlinStubBaseImpl<*>) {
    return createTargetedAnnotationStubs(annotations.map { AnnotationWithTarget(it, null) }, parent)
}

fun createTargetedAnnotationStubs(
    annotations: List<AnnotationWithTarget>,
    parent: KotlinStubBaseImpl<*>
) {
    if (annotations.isEmpty()) return

    annotations.forEach { annotation ->
        val (annotationWithArgs, target) = annotation
        createAnnotationStub(parent, annotationWithArgs, target)
    }
}

private fun createAnnotationStub(
    parent: StubElement<*>,
    annotationWithArgs: AnnotationWithArgs,
    target: AnnotationUseSiteTarget?
) {
    val annotationEntryStubImpl = KotlinAnnotationEntryStubImpl(
        parent,
        shortName = annotationWithArgs.classId.shortClassName.ref(),
        hasValueArguments = annotationWithArgs.args.isNotEmpty()
    )
    if (target != null) {
        KotlinAnnotationUseSiteTargetStubImpl(annotationEntryStubImpl, StringRef.fromString(target.name)!!)
    }
    val constructorCallee =
        KotlinPlaceHolderStubImpl<KtConstructorCalleeExpression>(annotationEntryStubImpl, KtStubElementTypes.CONSTRUCTOR_CALLEE)
    val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(constructorCallee, KtStubElementTypes.TYPE_REFERENCE)
    createStubForTypeName(annotationWithArgs.classId, typeReference)
    if (annotationWithArgs.args.isNotEmpty()) {
        val valueArgumentListStub =
            KotlinPlaceHolderStubImpl<KtValueArgumentList>(annotationEntryStubImpl, KtStubElementTypes.VALUE_ARGUMENT_LIST)
        for (entry in annotationWithArgs.args) {
            val constantValue = entry.value
            val valueArg = createValueArgWithName(valueArgumentListStub, entry.key)
            createAnnotationMappingByConstantValue(constantValue, valueArg)
        }
    }
}

/**
 * [org.jetbrains.kotlin.analysis.decompiler.stub.CallableClsStubBuilderKt.buildConstantInitializer]
 */
private fun createAnnotationMappingByConstantValue(
    constantValue: ConstantValue<*>,
    parent: StubElement<out PsiElement>
) {
    when (constantValue) {
        is EnumValue -> {
            createQualifiedReference(
                parent,
                ClassId(
                    constantValue.enumClassId.packageFqName,
                    constantValue.enumClassId.relativeClassName.child(constantValue.enumEntryName),
                    false
                )
            )
        }
        is KClassValue -> {
            when (val value = constantValue.value) {
                is KClassValue.Value.LocalClass -> error("Local classes are not reachable in annotation arguments, $value")
                is KClassValue.Value.NormalClass -> {
                    val classLiteralStub = KotlinClassLiteralExpressionStubImpl(parent)
                    createQualifiedReference(classLiteralStub, value.classId)
                }
            }
        }
        is BooleanValue -> {
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.BOOLEAN_CONSTANT,
                ConstantValueKind.BOOLEAN_CONSTANT,
                StringRef.fromString(constantValue.toString()))
        }
        is CharValue -> {
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.CHARACTER_CONSTANT,
                ConstantValueKind.CHARACTER_CONSTANT,
                StringRef.fromString(String.format("'\\u%04X'", constantValue.value.code))
            )
        }
        is IntegerValueConstant -> {
            //todo negative values as well as named constants may appear as constants; should be processed separately
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.INTEGER_CONSTANT,
                ConstantValueKind.INTEGER_CONSTANT,
                StringRef.fromString((constantValue.value as Number).toString())
            )
        }
        is DoubleValue, is FloatValue -> {
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.FLOAT_CONSTANT,
                ConstantValueKind.FLOAT_CONSTANT,
                StringRef.fromString(constantValue.value.toString())
            )
        }
        is NullValue -> {
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.NULL,
                ConstantValueKind.NULL,
                StringRef.fromString("null")
            )
        }
        is StringValue -> {
            buildStringTemplateExpressionStub(constantValue.value, parent)
        }
        is AnnotationValue -> {
            val valueArguments = constantValue.value.allValueArguments
            val classId = (constantValue.value as? AnnotationDescriptorWithClassId)?.classId ?: run {
                val fqName = constantValue.value.fqName ?: error("Missed annotation FqName")
                ClassId(fqName.parent(), fqName.shortName())
            }
            createAnnotationStub(parent, AnnotationWithArgs(classId, valueArguments), null)
        }
        is ArrayValue -> {
            val collectionLiteralStub = KotlinCollectionLiteralExpressionStubImpl(parent)
            for (value in constantValue.value) {
                createAnnotationMappingByConstantValue(value, collectionLiteralStub)
            }
        }
    }
}

internal fun buildStringTemplateExpressionStub(
    text: String,
    parent: StubElement<out PsiElement>
) {
    val stringStub =
        KotlinPlaceHolderStubImpl<KtStringTemplateExpression>(parent, KtStubElementTypes.STRING_TEMPLATE)
    if (text.isNotEmpty()) {
        text.split("\\").forEachIndexed { index, part ->
            if (index > 0) {
                KotlinPlaceHolderWithTextStubImpl<KtStringTemplateExpression>(
                    stringStub,
                    KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
                    "\\" + part.substring(0, 1)
                )
            }
            KotlinPlaceHolderWithTextStubImpl<KtStringTemplateExpression>(
                stringStub,
                KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
                if (index == 0) part else part.substring(1)
            )
        }
    }
}

private fun createValueArgWithName(
    parent: StubElement<out PsiElement>,
    name: Name
): KotlinValueArgumentStubImpl<KtValueArgument> {
    val valueArg = KotlinValueArgumentStubImpl(parent, KtStubElementTypes.VALUE_ARGUMENT, false)
    val argName = KotlinPlaceHolderStubImpl<KtValueArgumentName>(valueArg, KtStubElementTypes.VALUE_ARGUMENT_NAME)
    KotlinNameReferenceExpressionStubImpl(argName, name.ref())
    return valueArg
}

private fun createQualifiedReference(
    valueArg: StubElement<*>,
    classId: ClassId
) {
    val classLength = classId.relativeClassName.pathSegments().size
    createStubForPackageName(valueArg, classId.asSingleFqName(), classLength)
}

val MessageLite.annotatedCallableKind: AnnotatedCallableKind
    get() = when (this) {
        is ProtoBuf.Property -> AnnotatedCallableKind.PROPERTY
        is ProtoBuf.Function, is ProtoBuf.Constructor -> AnnotatedCallableKind.FUNCTION
        else -> throw IllegalStateException("Unsupported message: $this")
    }

fun Name.ref() = StringRef.fromString(this.asString())!!

fun FqName.ref() = StringRef.fromString(this.asString())!!
