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

package org.jetbrains.kotlin.fileClasses

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

object JvmFileClassUtil {
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    val JVM_PACKAGE_NAME: FqName = FqName("kotlin.jvm.JvmPackageName")
    private val JVM_PACKAGE_NAME_SHORT = JVM_PACKAGE_NAME.shortName().asString()

    private const val MULTIFILE_PART_NAME_DELIMITER = "__"

    fun getPartFqNameForDeserialized(descriptor: DeserializedMemberDescriptor): FqName =
            descriptor.getImplClassNameForDeserialized()?.fqNameForTopLevelClassMaybeWithDollars
            ?: error("No implClassName for $descriptor")

    @JvmStatic
    fun getFileClassInternalName(file: KtFile): String =
            getFileClassInfoNoResolve(file).fileClassFqName.internalNameWithoutInnerClasses

    @JvmStatic
    fun getFacadeClassInternalName(file: KtFile): String =
            getFileClassInfoNoResolve(file).facadeClassFqName.internalNameWithoutInnerClasses

    private fun manglePartName(facadeName: String, fileName: String): String =
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

    private fun getLiteralStringFromAnnotation(annotation: KtAnnotationEntry): String? {
        val argumentExpression = annotation.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        val stringTemplate = argumentExpression as? KtStringTemplateExpression ?: return null
        val singleEntry = stringTemplate.entries.singleOrNull() as? KtLiteralStringTemplateEntry ?: return null
        return singleEntry.text
    }
}

internal class ParsedJvmFileClassAnnotations(val jvmName: String?, val jvmPackageName: FqName?, val isMultifileClass: Boolean)

val KtFile.javaFileFacadeFqName: FqName
    get() {
        return CachedValuesManager.getCachedValue(this) {
            val facadeFqName =
                    if (isCompiled) packageFqName.child(Name.identifier(virtualFile.nameWithoutExtension))
                    else JvmFileClassUtil.getFileClassInfoNoResolve(this).facadeClassFqName

            if (!Name.isValidIdentifier(facadeFqName.shortName().identifier)) {
                LOG.error(
                    "An invalid fqName `$facadeFqName` with short name `${facadeFqName.shortName()}` is created for file `$name` " +
                            "(isCompiled = $isCompiled)"
                )
            }

            CachedValueProvider.Result(facadeFqName, this)
        }
    }

private val LOG = Logger.getInstance("JvmFileClassUtil")

fun KtDeclaration.isInsideJvmMultifileClassFile() = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(
        containingKtFile,
        JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT
) != null

val FqName.internalNameWithoutInnerClasses: String
    get() = JvmClassName.byFqNameWithoutInnerClasses(this).internalName
