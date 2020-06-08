/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isInterface
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.isCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmDefaultChecker(val jvmTarget: JvmTarget) : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val jvmDefaultMode = context.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

        descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_NOT_IN_INTERFACE.on(reportOn))
            } else if (jvmTarget == JvmTarget.JVM_1_6) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_JVM6_TARGET.on(reportOn, "JvmDefault"))
            } else if (!jvmDefaultMode.isEnabled) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(declaration, "JvmDefault"))
            }
            return@check
        }

        descriptor.annotations.findAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (jvmTarget == JvmTarget.JVM_1_6) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_JVM6_TARGET.on(reportOn, "JvmDefaultWithoutCompatibility"))
            } else if (!jvmDefaultMode.isEnabled) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(reportOn, "JvmDefaultWithoutCompatibility"))
            }
            return@check
        }

        if (descriptor is ClassDescriptor) {
            val hasDeclaredJvmDefaults =
                descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>().any {
                    it.kind.isReal && it.isCompiledToJvmDefault(jvmDefaultMode)
                }
            if (!hasDeclaredJvmDefaults && !checkJvmDefaultsInHierarchy(descriptor, jvmDefaultMode)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_THROUGH_INHERITANCE.on(declaration))
            }
        }


        if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return
        val memberDescriptor = descriptor as? CallableMemberDescriptor ?: return
        if (descriptor is PropertyAccessorDescriptor) return

        if (memberDescriptor.overriddenDescriptors.any { it.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME) }) {
            context.trace.report(ErrorsJvm.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE.on(declaration))
        } else if (jvmDefaultMode.isEnabled) {
            descriptor.overriddenDescriptors.flatMap { OverridingUtil.getOverriddenDeclarations(it) }.toSet().let {
                for (realDescriptor in OverridingUtil.filterOutOverridden(it)) {
                    if (realDescriptor is JavaMethodDescriptor && realDescriptor.modality != Modality.ABSTRACT) {
                        return context.trace.report(ErrorsJvm.NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT.on(declaration))
                    }
                }
            }
        }
    }

    private fun checkJvmDefaultsInHierarchy(descriptor: DeclarationDescriptor, jvmDefaultMode: JvmDefaultMode): Boolean {
        if (jvmDefaultMode.isEnabled) return true

        if (descriptor !is ClassDescriptor) return true

        return descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<CallableMemberDescriptor>()
            .all { memberDescriptor ->
                memberDescriptor.kind.isReal || OverridingUtil.filterOutOverridden(memberDescriptor.overriddenDescriptors.toSet()).all {
                    !isInterface(it.containingDeclaration) || !it.isCompiledToJvmDefault(jvmDefaultMode) || it.modality == Modality.ABSTRACT
                }
            }
    }

}