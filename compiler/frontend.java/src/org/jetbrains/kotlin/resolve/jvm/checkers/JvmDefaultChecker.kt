/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmDefaultChecker(val jvmTarget: JvmTarget) : DeclarationChecker {

    companion object {
        val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")

    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {

        descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_NOT_IN_INTERFACE.on(reportOn))
            } else if (jvmTarget == JvmTarget.JVM_1_6) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_JVM6_TARGET.on(reportOn))
            }
            return@check
        }

        if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return
        val memberDescriptor = descriptor as? CallableMemberDescriptor ?: return
        if (descriptor is PropertyAccessorDescriptor) return

        if (memberDescriptor.overriddenDescriptors.any { it.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME) }) {
            context.trace.report(ErrorsJvm.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE.on(declaration))
        }
    }
}