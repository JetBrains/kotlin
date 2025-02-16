/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmDefaultMode.ENABLE
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.LanguageVersionSettingsProvider
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultNoCompatibilityAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.isCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.util.getNonPrivateTraitMembersForDelegation

class JvmDefaultChecker(project: Project) : DeclarationChecker {
    private val ideService = LanguageVersionSettingsProvider.getInstance(project)

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val jvmDefaultMode = context.languageVersionSettings.getFlag(JvmAnalysisFlags.jvmDefaultMode)

        if (checkJvmCompatibilityAnnotations(descriptor, declaration, context, jvmDefaultMode)) return

        if (!jvmDefaultMode.isEnabled || descriptor !is ClassDescriptor || isInterface(descriptor) || isAnnotationClass(descriptor)) return
        // JvmDefaults members checks across class hierarchy:
        // 1. If in old scheme class have implicit override with different signature than overridden method (e.g. generic specialization)
        // report error because absent of it's can affect library ABI
        // 2. If it's mixed hierarchy with implicit override in base class and override one in inherited derived interface report error.
        // Otherwise the implicit class override would be used for dispatching method calls (but not more specialized)
        val performSpecializationCheck =
            jvmDefaultMode == JvmDefaultMode.ENABLE && !descriptor.hasJvmDefaultNoCompatibilityAnnotation() &&
                    //TODO: maybe remove this check for JVM compatibility
                    !(descriptor.modality !== Modality.OPEN && descriptor.modality !== Modality.ABSTRACT || descriptor.isEffectivelyPrivateApi)

        //Should we check clash with implicit class member (that comes from old compilation scheme) and specialization for compatibility mode
        // If specialization check is reported clash one shouldn't be reported
        if (descriptor.getSuperClassNotAny() == null && !performSpecializationCheck) return

        getNonPrivateTraitMembersForDelegation(
            descriptor,
            returnImplNotDelegate = true
        ).filter { (_, actualImplementation) -> actualImplementation.isCompiledToJvmDefaultWithProperMode(jvmDefaultMode) }
            .forEach { (inheritedMember, actualImplementation) ->
                if (actualImplementation is FunctionDescriptor && inheritedMember is FunctionDescriptor) {
                    checkSpecializationInCompatibilityMode(
                        inheritedMember, actualImplementation, context, declaration, performSpecializationCheck,
                    )
                } else if (actualImplementation is PropertyDescriptor && inheritedMember is PropertyDescriptor) {
                    val getterImpl = actualImplementation.getter
                    val getterInherited = inheritedMember.getter
                    if (getterImpl == null || getterInherited == null || jvmDefaultMode != ENABLE ||
                        checkSpecializationInCompatibilityMode(
                            getterInherited, getterImpl, context, declaration, performSpecializationCheck,
                        )
                    ) {
                        if (actualImplementation.isVar && inheritedMember.isVar) {
                            val setterImpl = actualImplementation.setter
                            val setterInherited = inheritedMember.setter
                            if (setterImpl != null && setterInherited != null) {
                                checkSpecializationInCompatibilityMode(
                                    setterInherited, setterImpl, context, declaration, performSpecializationCheck,
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun checkJvmCompatibilityAnnotations(
        descriptor: DeclarationDescriptor,
        declaration: KtDeclaration,
        context: DeclarationCheckerContext,
        jvmDefaultMode: JvmDefaultMode
    ): Boolean {
        descriptor.annotations.findAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (!jvmDefaultMode.isEnabled) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_IN_DECLARATION.on(reportOn, "JvmDefaultWithoutCompatibility"))
                return true
            }
        }

        descriptor.annotations.findAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME)?.let { annotationDescriptor ->
            val reportOn = DescriptorToSourceUtils.getSourceFromAnnotation(annotationDescriptor) ?: declaration
            if (jvmDefaultMode != JvmDefaultMode.NO_COMPATIBILITY) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION.on(reportOn))
                return true
            } else if (!isInterface(descriptor)) {
                context.trace.report(ErrorsJvm.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE.on(reportOn))
                return true
            }
        }

        return false
    }

    private fun checkSpecializationInCompatibilityMode(
        inheritedFun: FunctionDescriptor,
        actualImplementation: FunctionDescriptor,
        context: DeclarationCheckerContext,
        declaration: KtDeclaration,
        performSpecializationCheck: Boolean
    ): Boolean {
        if (!performSpecializationCheck || actualImplementation is JavaMethodDescriptor) return true
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

    private fun CallableMemberDescriptor.isCompiledToJvmDefaultWithProperMode(compilationDefaultMode: JvmDefaultMode) =
        isCompiledToJvmDefaultWithProperMode(ideService, compilationDefaultMode)

}

internal fun CallableMemberDescriptor.isCompiledToJvmDefaultWithProperMode(
    ideService: LanguageVersionSettingsProvider?,
    compilationDefaultMode: JvmDefaultMode
): Boolean {
    val jvmDefault =
        if (this is DeserializedDescriptor) compilationDefaultMode/*doesn't matter*/ else ideService?.getModuleLanguageVersionSettings(module)
            ?.getFlag(JvmAnalysisFlags.jvmDefaultMode) ?: compilationDefaultMode
    return isCompiledToJvmDefault(jvmDefault)
}
