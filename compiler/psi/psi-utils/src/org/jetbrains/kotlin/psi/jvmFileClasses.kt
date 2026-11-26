/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmFileClassNaming
import org.jetbrains.kotlin.name.Name

/**
 * The fully qualified name of the JVM class that holds the top-level declarations of this file.
 *
 * The `@JvmName`, `@JvmPackageName` and `@JvmMultifileClass` file annotations are matched by their short names, so the file is not
 * resolved.
 */
val KtFile.jvmFileClassFqName: FqName
    get() = jvmFileClassFqNames.fileClass

/**
 * The fully qualified name of the JVM class through which the top-level declarations of this file are accessed. It differs from
 * [jvmFileClassFqName] only if the file is a part of a multifile class.
 */
val KtFile.jvmFacadeClassFqName: FqName
    get() = jvmFileClassFqNames.facadeClass

private class JvmFileClassFqNames(val fileClass: FqName, val facadeClass: FqName)

private val KtFile.jvmFileClassFqNames: JvmFileClassFqNames
    get() = CachedValuesManager.getCachedValue(this) {
        CachedValueProvider.Result(computeJvmFileClassFqNames(this), this)
    }

private fun computeJvmFileClassFqNames(file: KtFile): JvmFileClassFqNames {
    if (file.isCompiled) {
        val classFqName = JvmFileClassNaming.fileClassFqName(file.packageFqName, file.name, isCompiled = true)
        return JvmFileClassFqNames(classFqName, classFqName)
    }

    val jvmName = file.findFileAnnotation("JvmName")?.singleStringArgument?.takeIf(Name::isValidIdentifier)
    val jvmPackageName = file.findFileAnnotation("JvmPackageName")?.singleStringArgument?.let(::FqName)

    val packageFqName = jvmPackageName ?: file.packageFqName
    val facadeClassName = jvmName ?: JvmFileClassNaming.fileClassName(file.name)
    val facadeClassFqName = packageFqName.child(Name.identifier(facadeClassName))

    // '@JvmMultifileClass' takes effect only together with '@JvmName' or '@JvmPackageName'
    val isMultifileClassPart = (jvmName != null || jvmPackageName != null) && file.findFileAnnotation("JvmMultifileClass") != null
    if (!isMultifileClassPart) {
        return JvmFileClassFqNames(facadeClassFqName, facadeClassFqName)
    }

    val partClassName = JvmFileClassNaming.multifileClassPartName(facadeClassName, file.name)
    return JvmFileClassFqNames(packageFqName.child(Name.identifier(partClassName)), facadeClassFqName)
}

private fun KtFile.findFileAnnotation(shortName: String): KtAnnotationEntry? {
    return fileAnnotationList?.annotationEntries?.firstOrNull {
        it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
    }
}

private val KtAnnotationEntry.singleStringArgument: String?
    get() {
        val stringTemplate = when (val argument = valueArguments.firstOrNull()) {
            null -> null
            is KtValueArgument -> argument.stringTemplateExpression
            else -> argument.getArgumentExpression() as? KtStringTemplateExpression
        }

        return (stringTemplate?.entries?.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
    }
