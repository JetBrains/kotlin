/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.java.DeprecationCausedByFunctionN
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DEPRECATED_FUNCTION_KEY
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

object BadInheritedJavaSignaturesChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return

        val badSignatureOverriddenDescriptor =
            descriptor.unsubstitutedMemberScope.getContributedDescriptors().firstNotNullResult(::findFirstBadJavaSignatureOverridden)

        if (badSignatureOverriddenDescriptor != null) {
            val reportOn =
                when (declaration) {
                    is KtClass -> declaration.nameIdentifier ?: declaration.getClassOrInterfaceKeyword()
                    is KtObjectDeclaration -> declaration.getObjectKeyword()
                    else -> null
                } ?: declaration

            val renderedDescriptor = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(badSignatureOverriddenDescriptor)
            context.trace.report(
                Errors.UNSUPPORTED.on(
                    reportOn,
                    "Inheritance of a Java member referencing '${JavaToKotlinClassMap.FUNCTION_N_FQ_NAME}': $renderedDescriptor"
                )
            )
        }
    }
}

private fun findFirstBadJavaSignatureOverridden(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
    if (descriptor !is CallableDescriptor) return null

    return descriptor.overriddenDescriptors.firstOrNull {
            overridden -> overridden.getUserData(DEPRECATED_FUNCTION_KEY) is DeprecationCausedByFunctionN
    }
}
