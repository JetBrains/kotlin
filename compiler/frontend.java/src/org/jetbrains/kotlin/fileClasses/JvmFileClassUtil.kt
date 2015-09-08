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

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.StringValue

public object JvmFileClassUtil {
    public val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    public val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    public val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    public val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    public @JvmStatic fun getFileClassInfo(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations?): JvmFileClassInfo =
            if (jvmFileClassAnnotations != null)
                getFileClassInfoForAnnotation(file, jvmFileClassAnnotations)
            else
                getDefaultFileClassInfo(file)

    public @JvmStatic fun getFileClassInfoForAnnotation(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): JvmFileClassInfo =
            if (jvmFileClassAnnotations.multipleFiles)
                JvmMultifileClassPartInfo(getHiddenPartFqName(file, jvmFileClassAnnotations),
                                          getFacadeFqName(file, jvmFileClassAnnotations))
            else
                JvmSimpleFileClassInfo(getFacadeFqName(file, jvmFileClassAnnotations), true)

    public @JvmStatic fun getDefaultFileClassInfo(file: JetFile): JvmFileClassInfo =
            JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(file.packageFqName, file.name), false)

    public @JvmStatic fun getFacadeFqName(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(jvmFileClassAnnotations.name))

    public @JvmStatic fun getHiddenPartFqName(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(manglePartName(jvmFileClassAnnotations.name, file.name)))

    public @JvmStatic fun manglePartName(facadeName: String, fileName: String): String =
            "${facadeName}__${PackagePartClassUtils.getFilePartShortName(fileName)}"

    public @JvmStatic fun parseJvmFileClass(annotations: Annotations): ParsedJmvFileClassAnnotations? {
        val jvmName = annotations.findAnnotation(JVM_NAME) ?: return null
        val jvmMultifileClass = annotations.findAnnotation(JVM_MULTIFILE_CLASS)
        return parseJvmFileClass(jvmName, jvmMultifileClass)
    }

    public @JvmStatic fun parseJvmFileClass(jvmName: AnnotationDescriptor, jvmMultifileClass: AnnotationDescriptor?): ParsedJmvFileClassAnnotations? {
        val jvmNameArgument = jvmName.allValueArguments.values().singleOrNull() ?: return null
        val name = (jvmNameArgument as? StringValue)?.value ?: return null
        val isMultifileClassPart = jvmMultifileClass != null
        return ParsedJmvFileClassAnnotations(name, isMultifileClassPart)
    }

    public @JvmStatic fun getFileClassInfoNoResolve(file: JetFile): JvmFileClassInfo =
            getFileClassInfo(file, parseJvmNameOnFileNoResolve(file))

    public @JvmStatic fun parseJvmNameOnFileNoResolve(file: JetFile): ParsedJmvFileClassAnnotations? {
        val jvmName = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT) ?: return null
        val nameExpr = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        val name = getLiteralStringFromRestrictedConstExpression(nameExpr) ?: return null
        val isMultifileClassPart = findAnnotationEntryOnFileNoResolve(file, JVM_MULTIFILE_CLASS_SHORT) != null
        return ParsedJmvFileClassAnnotations(name, isMultifileClassPart)
    }

    public @JvmStatic fun findAnnotationEntryOnFileNoResolve(file: JetFile, shortName: String): JetAnnotationEntry? =
            file.fileAnnotationList?.annotationEntries?.firstOrNull {
                it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
            }

    private @JvmStatic fun getLiteralStringFromRestrictedConstExpression(argumentExpression: JetExpression?): String? {
        val stringTemplate = argumentExpression as? JetStringTemplateExpression ?: return null
        val stringTemplateEntries = stringTemplate.entries
        if (stringTemplateEntries.size() != 1) return null
        val singleEntry = stringTemplateEntries[0] as? JetLiteralStringTemplateEntry ?: return null
        return singleEntry.text
    }

    public @JvmStatic fun collectFileAnnotations(file: JetFile, bindingContext: BindingContext): Annotations {
        val fileAnnotationsList = file.fileAnnotationList ?: return Annotations.EMPTY
        val annotationDescriptors = arrayListOf<AnnotationDescriptor>()
        for (annotationEntry in fileAnnotationsList.annotationEntries) {
            bindingContext.get(BindingContext.ANNOTATION, annotationEntry)?.let { annotationDescriptors.add(it) }
        }
        return AnnotationsImpl(annotationDescriptors)
    }
}

public class ParsedJmvFileClassAnnotations(public val name: String, public val multipleFiles: Boolean)
