/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.backend.utils.isInlineArrayConstructor
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.utils.classFqName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.JvmMultifileClassPartInfo
import org.jetbrains.kotlin.fileClasses.JvmSimpleFileClassInfo
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

val BirClass.isJvmInterface: Boolean
    get() = kind == ClassKind.ANNOTATION_CLASS || kind == ClassKind.INTERFACE

fun BirFile.getFileClassInfo(): JvmFileClassInfo =
    when (val fileEntry = this.fileEntry) {
        is PsiIrFileEntry ->
            JvmFileClassUtil.getFileClassInfoNoResolve(fileEntry.psiFile as KtFile)
        is NaiveSourceBasedFileEntryImpl ->
            getFileClassInfoFromBirFile(this, File(fileEntry.name).name)
        else ->
            error("unknown kind of file entry: $fileEntry")
    }

fun getFileClassInfoFromBirFile(file: BirFile, fileName: String): JvmFileClassInfo {
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

private fun parseJvmNameOnFileNoResolve(file: BirFile): ParsedJvmFileClassAnnotations? {
    val jvmNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JvmStandardClassIds.JVM_NAME_SHORT)
    val jvmName = jvmNameAnnotation?.let(::getLiteralStringFromAnnotation)?.takeIf(Name::isValidIdentifier)

    val jvmPackageNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JvmStandardClassIds.JVM_PACKAGE_NAME_SHORT)
    val jvmPackageName = jvmPackageNameAnnotation?.let(::getLiteralStringFromAnnotation)?.let(::FqName)

    if (jvmName == null && jvmPackageName == null) return null

    val isMultifileClass = findAnnotationEntryOnFileNoResolve(file, JvmStandardClassIds.JVM_MULTIFILE_CLASS_SHORT) != null

    return ParsedJvmFileClassAnnotations(jvmName, jvmPackageName, isMultifileClass)
}

private fun findAnnotationEntryOnFileNoResolve(file: BirFile, shortName: String): BirConstructorCall? =
    file.annotations.firstOrNull {
        it.type.classFqName?.shortName()?.asString() == shortName
    }

private fun getLiteralStringFromAnnotation(annotationCall: BirConstructorCall): String? {
    if (annotationCall.valueArguments.size < 1) return null
    return annotationCall.valueArguments[0]?.let {
        when {
            it is BirConst<*> && it.kind == IrConstKind.String -> it.value as String
            else -> null // TODO: getArgumentExpression().safeAs<KtStringTemplateExpression>()
        }
    }
}

internal class ParsedJvmFileClassAnnotations(val jvmName: String?, val jvmPackageName: FqName?, val isMultifileClass: Boolean)

context(JvmBirBackendContext)
fun BirFunction.isInlineFunctionCall(): Boolean =
    (!config.isInlineDisabled || typeParameters.any { it.isReified }) && (isInline || isInlineArrayConstructor())
