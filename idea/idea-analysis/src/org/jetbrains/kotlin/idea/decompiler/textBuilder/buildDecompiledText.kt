/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.decompiler.navigation.ByDescriptorIndexer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.renderer.ExcludedTypeAnnotations
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.types.isFlexible

private val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private val DECOMPILED_COMMENT_FOR_PARAMETER = "/* = compiled code */"
private val FLEXIBLE_TYPE_COMMENT = "/* platform type */"

fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
    excludedTypeAnnotationClasses = (ExcludedTypeAnnotations.annotationsForNullabilityAndMutability +
                                     KotlinBuiltIns.FQ_NAMES.extensionFunctionType +
                                     KotlinBuiltIns.FQ_NAMES.parameterName).toSet()
    alwaysRenderModifiers = true
    parameterNamesInFunctionalTypes = false // to support parameters names in decompiled text we need to load annotation arguments
}

fun buildDecompiledText(
        packageFqName: FqName,
        descriptors: List<DeclarationDescriptor>,
        descriptorRenderer: DescriptorRenderer,
        indexers: Collection<DecompiledTextIndexer<*>> = listOf(ByDescriptorIndexer)
): DecompiledText {
    val builder = StringBuilder()

    fun appendDecompiledTextAndPackageName() {
        builder.append("// IntelliJ API Decompiler stub source generated from a class file\n" + "// Implementation of methods is not available")
        builder.append("\n\n")
        if (!packageFqName.isRoot) {
            builder.append("package ").append(packageFqName).append("\n\n")
        }
    }

    val textIndex = DecompiledTextIndex(indexers)

    fun indexDescriptor(descriptor: DeclarationDescriptor, startOffset: Int, endOffset: Int) {
        textIndex.addToIndex(descriptor, TextRange(startOffset, endOffset))
    }

    fun appendDescriptor(descriptor: DeclarationDescriptor, indent: String, lastEnumEntry: Boolean? = null) {
        val startOffset = builder.length
        if (isEnumEntry(descriptor)) {
            for (annotation in descriptor.annotations) {
                builder.append(descriptorRenderer.renderAnnotation(annotation))
                builder.append(" ")
            }
            builder.append(descriptor.name.asString())
            builder.append(if (lastEnumEntry!!) ";" else ",")
        }
        else {
            builder.append(descriptorRenderer.render(descriptor).replace("= ...", DECOMPILED_COMMENT_FOR_PARAMETER))
        }
        var endOffset = builder.length

        if (descriptor is CallableDescriptor) {
            //NOTE: assuming that only return types can be flexible
            if (descriptor.returnType!!.isFlexible()) {
                builder.append(" ").append(FLEXIBLE_TYPE_COMMENT)
            }
        }

        if (descriptor is FunctionDescriptor || descriptor is PropertyDescriptor) {
            if ((descriptor as MemberDescriptor).modality != Modality.ABSTRACT) {
                if (descriptor is FunctionDescriptor) {
                    builder.append(" { ").append(DECOMPILED_CODE_COMMENT).append(" }")
                }
                else {
                    // descriptor instanceof PropertyDescriptor
                    builder.append(" ").append(DECOMPILED_CODE_COMMENT)
                }
                endOffset = builder.length
            }
        }
        else if (descriptor is ClassDescriptor && !isEnumEntry(descriptor)) {
            builder.append(" {\n")

            val subindent = indent + "    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                }
                else {
                    firstPassed = true
                }
            }

            val allDescriptors = descriptor.secondaryConstructors + descriptor.defaultType.memberScope.getContributedDescriptors()
            val (enumEntries, members) = allDescriptors.partition(::isEnumEntry)

            for ((index, enumEntry) in enumEntries.withIndex()) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(enumEntry, subindent, index == enumEntries.lastIndex)
            }

            val companionObject = descriptor.companionObjectDescriptor
            if (companionObject != null) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(companionObject, subindent)
            }

            for (member in members) {
                if (member.containingDeclaration != descriptor) {
                    continue
                }
                if (member == companionObject) {
                    continue
                }
                if (member is CallableMemberDescriptor
                    && member.kind != CallableMemberDescriptor.Kind.DECLARATION
                    //TODO: not synthesized and component like
                    && !DataClassDescriptorResolver.isComponentLike(member.name)) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
            endOffset = builder.length
        }

        builder.append("\n")
        indexDescriptor(descriptor, startOffset, endOffset)

        if (descriptor is ClassDescriptor) {
            val primaryConstructor = descriptor.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                indexDescriptor(primaryConstructor, startOffset, endOffset)
            }
        }
    }

    appendDecompiledTextAndPackageName()
    for (member in descriptors) {
        appendDescriptor(member, "")
        builder.append("\n")
    }

    return DecompiledText(builder.toString(), textIndex)
}
