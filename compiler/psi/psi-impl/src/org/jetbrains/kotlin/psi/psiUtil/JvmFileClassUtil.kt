/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * Copy of org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
 */
internal object JvmFileClassUtil {
    const val MULTIFILE_PART_NAME_DELIMITER = "__"
    const val JVM_PACKAGE_NAME_SHORT = "JvmPackageName"
    const val JVM_MULTIFILE_CLASS_SHORT = "JvmMultifileClass"
    const val JVM_NAME_SHORT: String = "JvmName"

    @JvmStatic
    fun manglePartName(facadeName: String, fileName: String): String =
        "$facadeName$MULTIFILE_PART_NAME_DELIMITER${PackagePartClassUtils.getFilePartShortName(fileName)}"

    @JvmStatic
    fun getFileClassInfoNoResolve(file: KtFile): JvmFileClassInfo {
        val parsedAnnotations = parseJvmNameOnFileNoResolve(file)
        val packageFqName = parsedAnnotations?.jvmPackageName ?: file.packageFqName
        return when {
            parsedAnnotations != null -> {
                val simpleName = parsedAnnotations.jvmName ?: PackagePartClassUtils.getFilePartShortName(file.name)
                val facadeClassFqName = packageFqName.child(Name.identifier(simpleName))
                when {
                    parsedAnnotations.isMultifileClass -> JvmMultifileClassPartInfo(
                        fileClassFqName = packageFqName.child(Name.identifier(manglePartName(simpleName, file.name))),
                        facadeClassFqName = facadeClassFqName
                    )

                    else -> JvmSimpleFileClassInfo(facadeClassFqName, true)
                }
            }

            else -> JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(packageFqName, file.name), false)
        }
    }

    private fun parseJvmNameOnFileNoResolve(file: KtFile): ParsedJvmFileClassAnnotations? {
        val jvmNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT)
        val jvmName = jvmNameAnnotation?.let(this::getLiteralStringFromAnnotation)?.takeIf(Name::isValidIdentifier)

        val jvmPackageNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_PACKAGE_NAME_SHORT)
        val jvmPackageName = jvmPackageNameAnnotation?.let(this::getLiteralStringFromAnnotation)?.let(::FqName)

        if (jvmName == null && jvmPackageName == null) return null

        val isMultifileClass = findAnnotationEntryOnFileNoResolve(file, JVM_MULTIFILE_CLASS_SHORT) != null

        return ParsedJvmFileClassAnnotations(jvmName, jvmPackageName, isMultifileClass)
    }

    @JvmStatic
    fun findAnnotationEntryOnFileNoResolve(file: KtFile, shortName: String): KtAnnotationEntry? =
        file.fileAnnotationList?.annotationEntries?.firstOrNull {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
        }

    fun getLiteralStringFromAnnotation(annotation: KtAnnotationEntry): String? {
        return getLiteralStringEntryFromAnnotation(annotation)?.text
    }

    fun getLiteralStringEntryFromAnnotation(annotation: KtAnnotationEntry): KtLiteralStringTemplateEntry? {
        val stringTemplateExpression = annotation.valueArguments.firstOrNull()?.run {
            when (this) {
                is KtValueArgument -> stringTemplateExpression
                else -> getArgumentExpression() as? KtStringTemplateExpression
            }
        } ?: return null

        return stringTemplateExpression.entries.singleOrNull() as? KtLiteralStringTemplateEntry
    }

    private class ParsedJvmFileClassAnnotations(val jvmName: String?, val jvmPackageName: FqName?, val isMultifileClass: Boolean)
}
