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

import com.google.protobuf.MessageLite
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForProto
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf


public object JvmFileClassUtil {
    public val JVM_NAME: FqName = FqName("kotlin.jvm.JvmName")
    public val JVM_NAME_SHORT: String = JVM_NAME.shortName().asString()

    public val JVM_MULTIFILE_CLASS: FqName = FqName("kotlin.jvm.JvmMultifileClass")
    public val JVM_MULTIFILE_CLASS_SHORT = JVM_MULTIFILE_CLASS.shortName().asString()

    @JvmStatic
    public fun getFileClassInfo(file: KtFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations?): JvmFileClassInfo =
            if (jvmFileClassAnnotations != null)
                getFileClassInfoForAnnotation(file, jvmFileClassAnnotations)
            else
                getDefaultFileClassInfo(file)

    @JvmStatic
    public fun getFileClassInfoForAnnotation(file: KtFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): JvmFileClassInfo =
            if (jvmFileClassAnnotations.multipleFiles)
                JvmMultifileClassPartInfo(getHiddenPartFqName(file, jvmFileClassAnnotations),
                                          getFacadeFqName(file, jvmFileClassAnnotations))
            else
                JvmSimpleFileClassInfo(getFacadeFqName(file, jvmFileClassAnnotations), true)

    @JvmStatic
    public fun getDefaultFileClassInfo(file: KtFile): JvmFileClassInfo =
            JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(file.packageFqName, file.name), false)

    @JvmStatic
    public fun getFacadeFqName(file: KtFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(jvmFileClassAnnotations.name))

    @JvmStatic
    public fun getPartFqNameForDeserializedCallable(callable: DeserializedCallableMemberDescriptor): FqName {
        val implClassName = getImplClassName(callable) ?: error("No implClassName for $callable")
        val packageFqName = (callable.containingDeclaration as PackageFragmentDescriptor).fqName
        return packageFqName.child(implClassName)
    }

    @JvmStatic
    public fun getImplClassName(callable: DeserializedCallableMemberDescriptor): Name? =
            callable.getImplClassNameForDeserialized()

    @JvmStatic
    public fun getImplClassName(proto: MessageLite, nameResolver: NameResolver): Name? =
            getImplClassNameForProto(proto, nameResolver)

    @JvmStatic
    public fun getHiddenPartFqName(file: KtFile, jvmFileClassAnnotations: ParsedJmvFileClassAnnotations): FqName =
            file.packageFqName.child(Name.identifier(manglePartName(jvmFileClassAnnotations.name, file.name)))

    @JvmStatic
    public fun manglePartName(facadeName: String, fileName: String): String =
            "${facadeName}__${PackagePartClassUtils.getFilePartShortName(fileName)}"

    @JvmStatic
    public fun getFileClassInfoNoResolve(file: KtFile): JvmFileClassInfo =
            getFileClassInfo(file, parseJvmNameOnFileNoResolve(file))

    @JvmStatic
    public fun parseJvmNameOnFileNoResolve(file: KtFile): ParsedJmvFileClassAnnotations? {
        val jvmName = findAnnotationEntryOnFileNoResolve(file, JVM_NAME_SHORT) ?: return null
        val nameExpr = jvmName.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
        val name = getLiteralStringFromRestrictedConstExpression(nameExpr) ?: return null
        if (!Name.isValidIdentifier(name)) return null
        val isMultifileClassPart = findAnnotationEntryOnFileNoResolve(file, JVM_MULTIFILE_CLASS_SHORT) != null
        return ParsedJmvFileClassAnnotations(name, isMultifileClassPart)
    }

    @JvmStatic
    public fun findAnnotationEntryOnFileNoResolve(file: KtFile, shortName: String): KtAnnotationEntry? =
            file.fileAnnotationList?.annotationEntries?.firstOrNull {
                it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == shortName
            }

    private @JvmStatic fun getLiteralStringFromRestrictedConstExpression(argumentExpression: KtExpression?): String? {
        val stringTemplate = argumentExpression as? KtStringTemplateExpression ?: return null
        val stringTemplateEntries = stringTemplate.entries
        if (stringTemplateEntries.size() != 1) return null
        val singleEntry = stringTemplateEntries[0] as? KtLiteralStringTemplateEntry ?: return null
        return singleEntry.text
    }

}

public class ParsedJmvFileClassAnnotations(public val name: String, public val multipleFiles: Boolean)

public val KtFile.javaFileFacadeFqName: FqName
    get() {
        return CachedValuesManager.getCachedValue(this) {
            val facadeFqName =
                    if (isCompiled) packageFqName.child(Name.identifier(virtualFile.nameWithoutExtension))
                    else JvmFileClassUtil.getFileClassInfoNoResolve(this).facadeClassFqName
            CachedValueProvider.Result(facadeFqName, this)
        }
    }

public fun KtDeclaration.isInsideJvmMultifileClassFile() = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(
        getContainingJetFile(),
        JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT
) != null
