/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.references.fe10.util.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

internal class InlineFunctionsCollector(
    private val project: Project,
    private val reifiedInlineFunctionsOnly: Boolean,
    private val acceptDeclaration: (KtDeclarationWithBody) -> Unit
) {
    fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
        if (resolvedCall == null) return

        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor is DeserializedSimpleFunctionDescriptor) return

        analyzeNextIfInline(descriptor)

        if (descriptor is PropertyDescriptor) {
            for (accessor in descriptor.accessors) {
                analyzeNextIfInline(accessor)
            }
        }
    }

    private fun analyzeNextIfInline(descriptor: CallableDescriptor) {
        if (!InlineUtil.isInline(descriptor) || reifiedInlineFunctionsOnly && !hasReifiedTypeParameters(descriptor)) {
            return
        }

        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
        if (declaration != null && declaration is KtDeclarationWithBody) {
            acceptDeclaration(declaration)
        }
    }

    private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
        return descriptor.typeParameters.any { it.isReified }
    }
}
