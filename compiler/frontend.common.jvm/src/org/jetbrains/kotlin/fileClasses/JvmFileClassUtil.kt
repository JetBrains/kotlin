/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fileClasses

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_SHORT
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_PACKAGE_NAME_SHORT
import org.jetbrains.kotlin.name.JvmStandardClassIds.MULTIFILE_PART_NAME_DELIMITER
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

object JvmFileClassUtil {
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    fun getPartFqNameForDeserialized(descriptor: DeserializedMemberDescriptor): FqName =
        descriptor.getImplClassNameForDeserialized()?.fqNameForTopLevelClassMaybeWithDollars
            ?: error("No implClassName for $descriptor")

    @JvmStatic
    fun getFileClassInternalName(file: KtFile): String =
        getFileClassInfoNoResolve(file).fileClassFqName.internalNameWithoutInnerClasses

    @JvmStatic
    fun getFacadeClassInternalName(file: KtFile): String =
        getFileClassInfoNoResolve(file).facadeClassFqName.internalNameWithoutInnerClasses

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

        val isMultifileClass = file.isJvmMultifileClassFile

        return ParsedJvmFileClassAnnotations(jvmName, jvmPackageName, isMultifileClass)
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

internal class ParsedJvmFileClassAnnotations(val jvmName: String?, val jvmPackageName: FqName?, val isMultifileClass: Boolean)

val KtFile.fileClassInfo: JvmFileClassInfo
    get() {
        return CachedValuesManager.getCachedValue(this) {
            CachedValueProvider.Result(JvmFileClassUtil.getFileClassInfoNoResolve(this), this)
        }
    }

val KtFile.javaFileFacadeFqName: FqName
    get() {
        val facadeFqName =
            if (isCompiled) packageFqName.child(Name.identifier(virtualFile.nameWithoutExtension))
            else this.fileClassInfo.facadeClassFqName

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
