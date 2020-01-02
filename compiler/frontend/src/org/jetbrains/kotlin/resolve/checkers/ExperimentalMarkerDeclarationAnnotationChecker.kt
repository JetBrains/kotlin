/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ExperimentalMarkerDeclarationAnnotationChecker(private val module: ModuleDescriptor) : AdditionalAnnotationChecker {
    private val WRONG_TARGETS_FOR_MARKER = setOf(KotlinTarget.EXPRESSION, KotlinTarget.FILE)

    override fun checkEntries(entries: List<KtAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        var isAnnotatedWithExperimental = false

        for (entry in entries) {
            val annotation = trace.bindingContext.get(BindingContext.ANNOTATION, entry)
            when (annotation?.fqName) {
                in ExperimentalUsageChecker.USE_EXPERIMENTAL_FQ_NAMES -> {
                    val annotationClasses =
                        annotation!!.allValueArguments[ExperimentalUsageChecker.USE_EXPERIMENTAL_ANNOTATION_CLASS]
                            .safeAs<ArrayValue>()?.value.orEmpty()
                    checkUseExperimentalUsage(annotationClasses, trace, entry)
                }
                in ExperimentalUsageChecker.EXPERIMENTAL_FQ_NAMES -> {
                    isAnnotatedWithExperimental = true
                }
            }
        }

        if (isAnnotatedWithExperimental) {
            checkMarkerTargets(entries, trace)
        }
    }

    private fun checkUseExperimentalUsage(annotationClasses: List<ConstantValue<*>>, trace: BindingTrace, entry: KtAnnotationEntry) {
        if (annotationClasses.isEmpty()) {
            trace.report(Errors.USE_EXPERIMENTAL_WITHOUT_ARGUMENTS.on(entry))
            return
        }

        for (annotationClass in annotationClasses) {
            val classDescriptor =
                (annotationClass as? KClassValue)?.getArgumentType(module)?.constructor?.declarationDescriptor as? ClassDescriptor
                    ?: continue
            val experimentality = with(ExperimentalUsageChecker) {
                classDescriptor.loadExperimentalityForMarkerAnnotation()
            }
            if (experimentality == null) {
                trace.report(Errors.USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER.on(entry, classDescriptor.fqNameSafe))
            }
        }
    }

    private fun checkMarkerTargets(entries: List<KtAnnotationEntry>, trace: BindingTrace) {
        val targetEntry =
            entries.associate { entry -> entry to trace.bindingContext.get(BindingContext.ANNOTATION, entry) }
                .entries
                .firstOrNull { (_, descriptor) -> descriptor != null && descriptor.fqName == KotlinBuiltIns.FQ_NAMES.target }
                    ?: return
        val (entry, descriptor) = targetEntry
        val allowedTargets = AnnotationChecker.loadAnnotationTargets(descriptor!!) ?: return
        val wrongTargets = allowedTargets.intersect(WRONG_TARGETS_FOR_MARKER)
        if (wrongTargets.isNotEmpty()) {
            trace.report(
                Errors.EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET.on(entry, wrongTargets.joinToString(transform = KotlinTarget::description))
            )
        }
    }
}
