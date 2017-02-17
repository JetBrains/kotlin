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

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

object JvmFileClassUtil {
    val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    const val MULTIFILE_PART_NAME_DELIMITER = "__"

    internal fun getFileClassInfo(file: KtFile, jvmFileClassAnnotations: ParsedJvmFileClassAnnotations?): JvmFileClassInfo =
            if (jvmFileClassAnnotations != null)
                getFileClassInfoForAnnotation(file, jvmFileClassAnnotations)
            else
                getDefaultFileClassInfo(file)

    private fun getFileClassInfoForAnnotation(file: KtFile, jvmFileClassAnnotations: ParsedJvmFileClassAnnotations): JvmFileClassInfo =
            if (jvmFileClassAnnotations.multipleFiles)
                JvmMultifileClassPartInfo(getHiddenPartFqName(file, jvmFileClassAnnotations),
                                          getFacadeFqName(file, jvmFileClassAnnotations))
            else
                JvmSimpleFileClassInfo(getFacadeFqName(file, jvmFileClassAnnotations), true)

    @JvmStatic fun getDefaultFileClassInfo(file: KtFile): JvmFileClassInfo =
            JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(file.packageFqName, file.name), false)

    private fun getFacadeFqName(file: KtFile, jvmFileClassAnnotations: ParsedJvmFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(jvmFileClassAnnotations.name))

    @JvmStatic fun getPartFqNameForDeserialized(deserializedMemberDescriptor: DeserializedMemberDescriptor): FqName {
        val implClassName = getImplClassName(deserializedMemberDescriptor) ?: error("No implClassName for $deserializedMemberDescriptor")
        val packageFqName = (deserializedMemberDescriptor.containingDeclaration as PackageFragmentDescriptor).fqName
        return packageFqName.child(implClassName)
    }

    @JvmStatic fun getImplClassName(deserializedMemberDescriptor: DeserializedMemberDescriptor): Name? =
            deserializedMemberDescriptor.getImplClassNameForDeserialized()

    private fun getHiddenPartFqName(file: KtFile, jvmFileClassAnnotations: ParsedJvmFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(manglePartName(jvmFileClassAnnotations.name, file.name)))

    @JvmStatic fun manglePartName(facadeName: String, fileName: String): String =
            "$facadeName$MULTIFILE_PART_NAME_DELIMITER${PackagePartClassUtils.getFilePartShortName(fileName)}"

    @JvmStatic fun getFileClassInfoNoResolve(file: KtFile): JvmFileClassInfo =
            getFileClassInfo(file, parseJvmNameOnFileNoResolve(file))

    internal fun parseJvmNameOnFileNoResolve(file: KtFile): ParsedJvmFileClassAnnotations? {
        val jvmName = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT) ?: return null
        val nameExpr = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        val name = getLiteralStringFromRestrictedConstExpression(nameExpr) ?: return null
        if (!Name.isValidIdentifier(name)) return null
        val isMultifileClassPart = findAnnotationEntryOnFileNoResolve(file, JVM_MULTIFILE_CLASS_SHORT) != null
        return ParsedJvmFileClassAnnotations(name, isMultifileClassPart)
    }

    @JvmStatic fun findAnnotationEntryOnFileNoResolve(file: KtFile, shortName: String): KtAnnotationEntry? =
            file.fileAnnotationList?.annotationEntries?.firstOrNull {
                it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
            }

    @JvmStatic private fun getLiteralStringFromRestrictedConstExpression(argumentExpression: KtExpression?): String? {
        val stringTemplate = argumentExpression as? KtStringTemplateExpression ?: return null
        val stringTemplateEntries = stringTemplate.entries
        if (stringTemplateEntries.size != 1) return null
        val singleEntry = stringTemplateEntries[0] as? KtLiteralStringTemplateEntry ?: return null
        return singleEntry.text
    }

    @JvmStatic fun isFromMultifileClass(declarationElement: KtElement, descriptor: DeclarationDescriptor): Boolean {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(declarationElement.containingKtFile)
            return fileClassInfo.withJvmMultifileClass
        }
        return false
    }
}

internal class ParsedJvmFileClassAnnotations(val name: String, val multipleFiles: Boolean)

val KtFile.javaFileFacadeFqName: FqName
    get() {
        return CachedValuesManager.getCachedValue(this) {
            val facadeFqName =
                    if (isCompiled) packageFqName.child(Name.identifier(virtualFile.nameWithoutExtension))
                    else JvmFileClassUtil.getFileClassInfoNoResolve(this).facadeClassFqName
            CachedValueProvider.Result(facadeFqName, this)
        }
    }

fun KtDeclaration.isInsideJvmMultifileClassFile() = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(
        containingKtFile,
        JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT
) != null
