/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmDefaultChecker(val jvmTarget: JvmTarget) : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val enableJvmDefault = context.languageVersionSettings.getFlag(AnalysisFlag.enableJvmDefault)

        descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_NOT_IN_INTERFACE.on(reportOn))
            } else if (jvmTarget == JvmTarget.JVM_1_6) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_JVM6_TARGET.on(reportOn))
            } else if (!enableJvmDefault) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(declaration))
            }
            return@check
        }

        if (descriptor is ClassDescriptor) {
            val hasDeclaredJvmDefaults =
                descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>().any {
                    it.kind.isReal && it.hasJvmDefaultAnnotation()
                }
            if (!hasDeclaredJvmDefaults && !checkJvmDefaultThroughInheritance(descriptor, enableJvmDefault)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_THROUGH_INHERITANCE.on(declaration))
            }
        }


        if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return
        val memberDescriptor = descriptor as? CallableMemberDescriptor ?: return
        if (descriptor is PropertyAccessorDescriptor) return

        if (memberDescriptor.overriddenDescriptors.any { it.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME) }) {
            context.trace.report(ErrorsJvm.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE.on(declaration))
        } else if (enableJvmDefault) {
            descriptor.overriddenDescriptors.flatMap { OverridingUtil.getOverriddenDeclarations(it) }.toSet().let {
                for (realDescriptor in OverridingUtil.filterOutOverridden(it)) {
                    if (realDescriptor is JavaMethodDescriptor && realDescriptor.modality != Modality.ABSTRACT) {
                        return context.trace.report(ErrorsJvm.NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT.on(declaration))
                    }
                }
            }
        }
    }

    private fun checkJvmDefaultThroughInheritance(descriptor: DeclarationDescriptor, enableJvmDefault: Boolean): Boolean {
        if (enableJvmDefault) return true

        if (descriptor !is ClassDescriptor) return true

        if (!DescriptorUtils.isInterface(descriptor) &&
            !DescriptorUtils.isAnnotationClass(descriptor)
        ) {
            return descriptor.getSuperInterfaces().all {
                checkJvmDefaultThroughInheritance(it, enableJvmDefault)
            }
        }


        return descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>().all {
            !it.hasJvmDefaultAnnotation()
        }
    }
}