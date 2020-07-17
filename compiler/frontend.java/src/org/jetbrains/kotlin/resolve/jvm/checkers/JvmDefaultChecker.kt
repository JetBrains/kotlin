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
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.jvm.annotations.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.util.getNonPrivateTraitMembersForDelegation

class JvmDefaultChecker(val jvmTarget: JvmTarget) : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val jvmDefaultMode = context.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

        descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!isInterface(descriptor.containingDeclaration)) {
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


        if (isInterface(descriptor.containingDeclaration)) {
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
        } else if (jvmDefaultMode.isCompatibility &&
            !isInterface(descriptor) &&
            !isAnnotationClass(descriptor) &&
            descriptor is ClassDescriptor &&
            !descriptor.hasJvmDefaultNoCompatibilityAnnotation()
        ) {
            val modality = descriptor.modality
            //TODO: maybe remove this check for jvm compatibility
            if (modality !== Modality.OPEN && modality !== Modality.ABSTRACT || descriptor.isEffectivelyPrivateApi) return
            for ((inheritedMember, actualImplementation) in getNonPrivateTraitMembersForDelegation(
                descriptor,
                returnImplNotDelegate = true
            )) {
                if (actualImplementation.isCallableMemberCompiledToJvmDefault(jvmDefaultMode)) {
                    if (actualImplementation is FunctionDescriptor && inheritedMember is FunctionDescriptor) {
                        processMember(inheritedMember, actualImplementation, context, declaration)
                    } else if (actualImplementation is PropertyDescriptor && inheritedMember is PropertyDescriptor) {
                        val getterImpl = actualImplementation.getter
                        val getterInherited = inheritedMember.getter
                        if (getterImpl == null || getterInherited == null || processMember(getterImpl, getterImpl, context, declaration)) {
                            if (actualImplementation.isVar && inheritedMember.isVar) {
                                val setterImpl = actualImplementation.setter
                                val setterInherited = inheritedMember.setter
                                if (setterImpl != null && setterInherited != null) {
                                    processMember(setterImpl, setterImpl, context, declaration)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processMember(
        inheritedFun: FunctionDescriptor,
        actualImplementation: FunctionDescriptor,
        context: DeclarationCheckerContext,
        declaration: KtDeclaration
    ): Boolean {
        val inheritedSignature = inheritedFun.computeJvmDescriptor(withReturnType = true, withName = false)
        val originalImplementation = actualImplementation.original
        val actualSignature = originalImplementation.computeJvmDescriptor(withReturnType = true, withName = false)
        if (inheritedSignature != actualSignature) {
            //NB: this diagnostics should be a bit tuned, see box/jvm8/defaults/allCompatibility/kt14243_2.kt for details
            context.trace.report(
                ErrorsJvm.EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE.on(
                    declaration,
                    getDirectMember(inheritedFun),
                    getDirectMember(originalImplementation)
                )
            )
            return false
        }
        return true
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