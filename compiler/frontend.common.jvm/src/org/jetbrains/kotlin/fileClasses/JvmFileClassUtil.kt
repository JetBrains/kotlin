/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fileClasses

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmFileClassNaming
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_SHORT
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_PACKAGE_NAME_SHORT
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

object JvmFileClassUtil {
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    @JvmStatic
    fun getFileClassInternalName(file: KtFile): String =
        getFileClassInfoNoResolve(file).fileClassFqName.internalNameWithoutInnerClasses

    @JvmStatic
    fun manglePartName(facadeName: String, fileName: String): String =
        JvmFileClassNaming.multifileClassPartName(facadeName, fileName)

    @JvmStatic
    fun getFileClassInfoNoResolve(file: KtFile): JvmFileClassInfo {
        val fileClassFqName = file.jvmFileClassFqName
        val facadeClassFqName = file.jvmFacadeClassFqName
        return when {
            fileClassFqName != facadeClassFqName -> JvmMultifileClassPartInfo(fileClassFqName, facadeClassFqName)
            else -> JvmSimpleFileClassInfo(fileClassFqName, withJvmName = hasJvmNameOrJvmPackageName(file))
        }
    }

    private fun hasJvmNameOrJvmPackageName(file: KtFile): Boolean {
        val jvmNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT)
        val jvmName = jvmNameAnnotation?.let(this::getLiteralStringFromAnnotation)?.takeIf(Name::isValidIdentifier)
        if (jvmName != null) return true

        val jvmPackageNameAnnotation = findAnnotationEntryOnFileNoResolve(file, JVM_PACKAGE_NAME_SHORT)
        return jvmPackageNameAnnotation?.let(this::getLiteralStringFromAnnotation) != null
    }

    @JvmStatic
    fun findAnnotationEntryOnFileNoResolve(file: KtFile, shortName: String): KtAnnotationEntry? =
        file.fileAnnotationList?.annotationEntries?.firstOrNull {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
        }

    @Suppress("MemberVisibilityCanBePrivate")
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
}

val KtFile.fileClassInfo: JvmFileClassInfo
    get() {
        return CachedValuesManager.getCachedValue(this) {
            CachedValueProvider.Result(JvmFileClassUtil.getFileClassInfoNoResolve(this), this)
        }
    }

val KtFile.javaFileFacadeFqName: FqName
    get() {
        val facadeFqName = jvmFacadeClassFqName

        if (!Name.isValidIdentifier(facadeFqName.shortName().identifier)) {
            LOG.error(
                "An invalid fqName `$facadeFqName` with short name `${facadeFqName.shortName()}` is created for file `$name` " +
                        "(isCompiled = $isCompiled)"
            )
        }
        return facadeFqName
    }

val KtFile.isJvmMultifileClassFile: Boolean
    get() = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(this, JVM_MULTIFILE_CLASS_SHORT) != null

private val LOG = Logger.getInstance("JvmFileClassUtil")

fun KtDeclaration.isInsideJvmMultifileClassFile() = containingKtFile.isJvmMultifileClassFile

val FqName.internalNameWithoutInnerClasses: String
    get() = JvmClassName.byFqNameWithoutInnerClasses(this).internalName
