/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.jetbrains.kotlin.analysis.decompiler.stub.COMPILED_DEFAULT_PARAMETER_VALUE
import org.jetbrains.kotlin.analysis.decompiler.stub.computeParameterName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val DECOMPILED_COMMENT_FOR_PARAMETER = "/* = compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"
private const val DECOMPILED_CONTRACT_STUB = "contract { /* compiled contract */ }"

fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
    excludedTypeAnnotationClasses = emptySet()
    alwaysRenderModifiers = true
    parameterNamesInFunctionalTypes = false // to support parameters names in decompiled text we need to load annotation arguments
    defaultParameterValueRenderer = { _ -> COMPILED_DEFAULT_PARAMETER_VALUE }
    includePropertyConstant = true
    propertyConstantRenderer = { _ -> COMPILED_DEFAULT_INITIALIZER }
}

internal fun CallableMemberDescriptor.mustNotBeWrittenToDecompiledText(): Boolean {
    return when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION, CallableMemberDescriptor.Kind.DELEGATION -> false
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> true
        CallableMemberDescriptor.Kind.SYNTHESIZED -> {
            // Of all synthesized functions, only `component*` functions are rendered (for historical reasons)
            !DataClassDescriptorResolver.isComponentLike(name) && name !in listOf(
                OperatorNameConventions.EQUALS,
                StandardNames.HASHCODE_NAME,
                OperatorNameConventions.TO_STRING
            )
        }
    }
}

fun buildDecompiledText(
    packageFqName: FqName,
    descriptors: List<DeclarationDescriptor>,
    descriptorRenderer: DescriptorRenderer,
): DecompiledText {
    val builder = StringBuilder()

    fun appendDecompiledTextAndPackageName() {
        builder.append("// IntelliJ API Decompiler stub source generated from a class file\n" + "// Implementation of methods is not available")
        builder.append("\n\n")
        if (!packageFqName.isRoot) {
            builder.append("package ").append(packageFqName.render()).append("\n\n")
        }
    }

    fun appendDescriptor(descriptor: DeclarationDescriptor, indent: String, lastEnumEntry: Boolean? = null) {
        if (isEnumEntry(descriptor)) {
            for (annotation in descriptor.annotations) {
                builder.append(descriptorRenderer.renderAnnotation(annotation))
                builder.append(" ")
            }
            builder.append(descriptor.name.asString().quoteIfNeeded())
            builder.append(if (lastEnumEntry!!) ";" else ",")
        } else {
            builder.append(descriptorRenderer.render(descriptor).replace("= ...", DECOMPILED_COMMENT_FOR_PARAMETER))
        }

        if (descriptor is CallableDescriptor) {
            //NOTE: assuming that only return types can be flexible
            if (descriptor.returnType!!.isFlexible()) {
                builder.append(" ").append(FLEXIBLE_TYPE_COMMENT)
            }
        }

        if (descriptor is FunctionDescriptor || descriptor is PropertyDescriptor) {
            if ((descriptor as MemberDescriptor).modality != Modality.ABSTRACT) {
                if (descriptor is FunctionDescriptor) {
                    with(builder) {
                        append(" { ")
                        if (descriptor.getUserData(ContractProviderKey)?.getContractDescription() != null) {
                            append(DECOMPILED_CONTRACT_STUB).append("; ")
                        }
                        append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                } else {
                    // descriptor instanceof PropertyDescriptor
                    builder.append(" ").append(DECOMPILED_CODE_COMMENT)
                }
            }
            if (descriptor is PropertyDescriptor) {
                for (accessor in descriptor.accessors) {
                    if (accessor.isDefault) continue
                    builder.append("\n$indent    ")
                    builder.append(accessor.visibility.internalDisplayName).append(" ")
                    builder.append(accessor.modality.name.toLowerCaseAsciiOnly()).append(" ")
                    if (accessor.isExternal) {
                        builder.append("external ")
                    }
                    for (annotation in accessor.annotations) {
                        builder.append(descriptorRenderer.renderAnnotation(annotation))
                        builder.append(" ")
                    }
                    if (accessor is PropertyGetterDescriptor) {
                        builder.append("get")
                    } else if (accessor is PropertySetterDescriptor) {
                        builder.append("set(")
                        val parameterDescriptor = accessor.valueParameters[0]
                        for (annotation in parameterDescriptor.annotations) {
                            builder.append(descriptorRenderer.renderAnnotation(annotation))
                            builder.append(" ")
                        }
                        val parameterName = computeParameterName(parameterDescriptor.name)
                        builder.append(parameterName.asString()).append(": ")
                            .append(descriptorRenderer.renderType(parameterDescriptor.type))
                        builder.append(")")
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                }
            }
        } else if (descriptor is ClassDescriptor && !isEnumEntry(descriptor)) {
            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
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
                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
        }

        builder.append("\n")
    }

    appendDecompiledTextAndPackageName()
    for (member in descriptors) {
        appendDescriptor(member, "")
        builder.append("\n")
    }

    return DecompiledText(builder.toString())
}
