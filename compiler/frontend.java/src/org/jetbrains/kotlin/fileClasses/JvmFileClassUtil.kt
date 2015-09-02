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
    public val JVM_NAME: FqName = FqName("kotlin.jvm.jvmName")
    public val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    // TODO @JvmMultifileClass

    public @jvmStatic fun getFileClassInfo(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations?): JvmFileClassInfo =
            if (jvmFileClassAnnotations != null)
                getFileClassInfoForAnnotation(file, jvmFileClassAnnotations)
            else
                getDefaultFileClassInfo(file)

    public @jvmStatic fun getFileClassInfoForAnnotation(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): JvmFileClassInfo =
            if (jvmFileClassAnnotations.multipleFiles)
                JvmMultifileFacadePartInfo(getHiddenPartFqName(file, jvmFileClassAnnotations),
                                           getFacadeFqName(file, jvmFileClassAnnotations))
            else
                JvmFileFacadeInfo(getFacadeFqName(file, jvmFileClassAnnotations))

    public @jvmStatic fun getDefaultFileClassInfo(file: JetFile): JvmFileClassInfo =
            JvmFileFacadeInfo(PackagePartClassUtils.getPackagePartFqName(file.packageFqName, file.name))

    public @jvmStatic fun getFacadeFqName(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(jvmFileClassAnnotations.name))

    public @jvmStatic fun getHiddenPartFqName(file: JetFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(manglePartName(jvmFileClassAnnotations.name, file.name)))

    public @jvmStatic fun manglePartName(facadeName: String, fileName: String): String =
            "${facadeName}__${PackagePartClassUtils.getFilePartShortName(fileName)}"

    public @jvmStatic fun parseJvmFileClass(annotations: Annotations): ParsedJmvFileClassAnnotations? {
        val jvmName = annotations.findAnnotation(JVM_NAME)
        // TODO @JvmMultifileClass
        return if (jvmName != null) parseJvmFileClass(jvmName) else null
    }

    public @jvmStatic fun parseJvmFileClass(jvmName: AnnotationDescriptor): ParsedJmvFileClassAnnotations {
        val name = jvmName.allValueArguments.values().firstOrNull()?.let { (it as? StringValue)?.value }
        return ParsedJmvFileClassAnnotations(name!!, false)
    }

    public @jvmStatic fun getFileClassInfoNoResolve(file: JetFile): JvmFileClassInfo =
            getFileClassInfo(file, parseJvmNameOnFileNoResolve(file))

    public @jvmStatic fun parseJvmNameOnFileNoResolve(file: JetFile): ParsedJmvFileClassAnnotations? =
            findJvmNameOnFileNoResolve(file)?.let { parseJvmNameOnFileNoResolve(it) }

    public @jvmStatic fun findJvmNameOnFileNoResolve(file: JetFile): JetAnnotationEntry? =
            file.fileAnnotationList?.annotationEntries?.firstOrNull {
                it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == JVM_NAME_SHORT
            }

    public @jvmStatic fun parseJvmNameOnFileNoResolve(annotationEntry: JetAnnotationEntry): ParsedJmvFileClassAnnotations? {
        val nameExpr = annotationEntry.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        val name = getLiteralStringFromRestrictedConstExpression(nameExpr)
        return name?.let { ParsedJmvFileClassAnnotations(it, false) }
    }

    private @jvmStatic fun getLiteralStringFromRestrictedConstExpression(argumentExpression: JetExpression?): String? {
        val stringTemplate = argumentExpression as? JetStringTemplateExpression ?: return null
        val stringTemplateEntries = stringTemplate.entries
        if (stringTemplateEntries.size() != 1) return null
        val singleEntry = stringTemplateEntries[0] as? JetLiteralStringTemplateEntry ?: return null
        return singleEntry.text
    }

    public @jvmStatic fun collectFileAnnotations(file: JetFile, bindingContext: BindingContext): Annotations {
        val fileAnnotationsList = file.fileAnnotationList ?: return Annotations.EMPTY
        val annotationDescriptors = arrayListOf<AnnotationDescriptor>()
        for (annotationEntry in fileAnnotationsList.annotationEntries) {
            bindingContext.get(BindingContext.ANNOTATION, annotationEntry)?.let { annotationDescriptors.add(it) }
        }
        return AnnotationsImpl(annotationDescriptors)
    }
}

public class ParsedJmvFileClassAnnotations(public val name: String, public val multipleFiles: Boolean)
