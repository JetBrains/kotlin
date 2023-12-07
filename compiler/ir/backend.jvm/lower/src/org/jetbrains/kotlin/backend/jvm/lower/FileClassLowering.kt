/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.JvmMultifileClassPartInfo
import org.jetbrains.kotlin.fileClasses.JvmSimpleFileClassInfo
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_SHORT
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_NAME_SHORT
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_PACKAGE_NAME_SHORT
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal val fileClassPhase = makeIrModulePhase(
    ::FileClassLowering,
    name = "FileClass",
    description = "Put file level function and property declaration into a class",
)

private class FileClassLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val classes = ArrayList<IrClass>()
        val fileClassMembers = ArrayList<IrDeclaration>()

        irFile.declarations.forEach {
            when (it) {
                is IrScript -> {
                }
                is IrClass -> classes.add(it)
                else -> fileClassMembers.add(it)
            }
        }

        // TODO FirMetadataSource.File
        if (fileClassMembers.isEmpty() && (irFile.metadata as? DescriptorMetadataSource.File)?.descriptors.isNullOrEmpty()) return

        val irFileClass = createFileClass(irFile, fileClassMembers)
        classes.add(irFileClass)

        irFile.declarations.clear()
        irFile.declarations.addAll(classes)
    }

    private fun createFileClass(irFile: IrFile, fileClassMembers: List<IrDeclaration>): IrClass {
        val fileEntry = irFile.fileEntry
        val fileClassInfo = irFile.getFileClassInfo()
        val isMultifilePart = fileClassInfo.withJvmMultifileClass

        val onlyPrivateDeclarationsAndFeatureIsEnabled =
            context.config.languageVersionSettings.supportsFeature(LanguageFeature.PackagePrivateFileClassesWithAllPrivateMembers) && fileClassMembers
                .all {
                    val isPrivate = it is IrDeclarationWithVisibility && DescriptorVisibilities.isPrivate(it.visibility)
                    val isInlineOnly = it.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)
                    isPrivate || isInlineOnly
                }

        val fileClassOrigin =
            if (!isMultifilePart || context.config.languageVersionSettings.getFlag(JvmAnalysisFlags.inheritMultifileParts))
                IrDeclarationOrigin.FILE_CLASS
            else
                IrDeclarationOrigin.SYNTHETIC_FILE_CLASS
        return context.irFactory.createClass(
            startOffset = 0,
            endOffset = fileEntry.maxOffset,
            origin = fileClassOrigin,
            name = fileClassInfo.fileClassFqName.shortName(),
            visibility = if (isMultifilePart || onlyPrivateDeclarationsAndFeatureIsEnabled)
                JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            else
                DescriptorVisibilities.PUBLIC,
            symbol = IrClassSymbolImpl(),
            kind = ClassKind.CLASS,
            modality = Modality.FINAL,
        ).apply {
            superTypes = listOf(context.irBuiltIns.anyType)
            parent = irFile
            declarations.addAll(fileClassMembers)
            createImplicitParameterDeclarationWithWrappedDescriptor()
            for (member in fileClassMembers) {
                member.parent = this
                if (member is IrProperty) {
                    member.getter?.let { it.parent = this }
                    member.setter?.let { it.parent = this }
                    member.backingField?.let { it.parent = this }
                }
            }

            annotations =
                if (isMultifilePart) irFile.annotations.filterNot {
                    it.symbol.owner.parentAsClass.hasEqualFqName(JvmFileClassUtil.JVM_NAME)
                }
                else irFile.annotations

            metadata = irFile.metadata

            val partClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.fileClassFqName)
            val facadeClassType =
                if (isMultifilePart) AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.facadeClassFqName)
                else null
            context.state.factory.packagePartRegistry.addPart(irFile.packageFqName, partClassType.internalName, facadeClassType?.internalName)

            if (fileClassInfo.fileClassFqName != fqNameWhenAvailable) {
                context.classNameOverride[this] = JvmClassName.byInternalName(partClassType.internalName)
            }

            if (facadeClassType != null) {
                val jvmClassName = JvmClassName.byInternalName(facadeClassType.internalName)
                context.multifileFacadesToAdd.getOrPut(jvmClassName) { ArrayList() }.add(this)
            }
        }
    }
}

fun IrFile.getFileClassInfo(): JvmFileClassInfo =
    when (val fileEntry = this.fileEntry) {
        is PsiIrFileEntry ->
            JvmFileClassUtil.getFileClassInfoNoResolve(fileEntry.psiFile as KtFile)
        is NaiveSourceBasedFileEntryImpl ->
            getFileClassInfoFromIrFile(this, File(fileEntry.name).name)
        else ->
            error("unknown kind of file entry: $fileEntry")
    }


fun getFileClassInfoFromIrFile(file: IrFile, fileName: String): JvmFileClassInfo {
    val parsedAnnotations = parseJvmNameOnFileNoResolve(file)
    val packageFqName = parsedAnnotations?.jvmPackageName ?: file.packageFqName
    return when {
        parsedAnnotations != null -> {
            val simpleName = parsedAnnotations.jvmName ?: PackagePartClassUtils.getFilePartShortName(fileName)
            val facadeClassFqName = packageFqName.child(Name.identifier(simpleName))
            when {
                parsedAnnotations.isMultifileClass -> JvmMultifileClassPartInfo(
                    fileClassFqName = packageFqName.child(Name.identifier(JvmFileClassUtil.manglePartName(simpleName, fileName))),
                    facadeClassFqName = facadeClassFqName
                )
                else -> JvmSimpleFileClassInfo(facadeClassFqName, true)
            }
        }
        else -> JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(packageFqName, fileName), false)
    }
}

private fun parseJvmNameOnFileNoResolve(file: IrFile): ParsedJvmFileClassAnnotations? {
    val jvmNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT)
    val jvmName = jvmNameAnnotation?.let(::getLiteralStringFromAnnotation)?.takeIf(Name::isValidIdentifier)

    val jvmPackageNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_PACKAGE_NAME_SHORT)
    val jvmPackageName = jvmPackageNameAnnotation?.let(::getLiteralStringFromAnnotation)?.let(::FqName)

    if (jvmName == null && jvmPackageName == null) return null

    val isMultifileClass = findAnnotationEntryOnFileNoResolve(file, JVM_MULTIFILE_CLASS_SHORT) != null

    return ParsedJvmFileClassAnnotations(jvmName, jvmPackageName, isMultifileClass)
}

private fun findAnnotationEntryOnFileNoResolve(file: IrFile, shortName: String): IrConstructorCall? =
    file.annotations.firstOrNull {
        it.type.classFqName?.shortName()?.asString() == shortName
    }

private fun getLiteralStringFromAnnotation(annotationCall: IrConstructorCall): String? {
    if (annotationCall.valueArgumentsCount < 1) return null
    return annotationCall.getValueArgument(0)?.let {
        when {
            it is IrConst<*> && it.kind == IrConstKind.String -> it.value as String
            else -> null // TODO: getArgumentExpression().safeAs<KtStringTemplateExpression>()
        }
    }
}

internal class ParsedJvmFileClassAnnotations(val jvmName: String?, val jvmPackageName: FqName?, val isMultifileClass: Boolean)
