/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotatedWithKotlinRepeatable
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformAnnotationFeaturesSupport
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class RepeatableAnnotationChecker(
    languageVersionSettings: LanguageVersionSettings,
    private val jvmTarget: JvmTarget,
    private val platformAnnotationFeaturesSupport: JvmPlatformAnnotationFeaturesSupport,
) : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (entries.isEmpty()) return

        val annotations = entries.mapNotNull { entry ->
            val descriptor = trace.get(BindingContext.ANNOTATION, entry)
            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()
            if (descriptor != null) {
                ResolvedAnnotation(entry, descriptor, useSiteTarget)
            } else null
        }

        checkRepeatedEntries(annotations, trace)
    }

    private fun checkRepeatedEntries(annotations: List<ResolvedAnnotation>, trace: BindingTrace) {
        val entryTypesWithAnnotations = hashMapOf<FqName, MutableList<AnnotationUseSiteTarget?>>()

        for ((entry, descriptor, useSiteTarget) in annotations) {
            val fqName = descriptor.fqName ?: continue
            val classDescriptor = descriptor.annotationClass ?: continue

            val existingTargetsForAnnotation = entryTypesWithAnnotations.getOrPut(fqName) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation
                    || (existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) })

            if (duplicateAnnotation
                && isRepeatableAnnotation(classDescriptor)
                && classDescriptor.getAnnotationRetention() != KotlinRetention.SOURCE
            ) {
                when {
                    jvmTarget == JvmTarget.JVM_1_6 -> {
                        trace.report(ErrorsJvm.REPEATED_ANNOTATION_TARGET6.on(entry))
                    }
                    languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations) -> {
                        // It's not allowed to have both a repeated annotation (applied more than once) and its container
                        // on the same element. See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.5.
                        val explicitContainer = resolveContainerAnnotation(classDescriptor)
                        if (explicitContainer != null && annotations.any { it.descriptor.fqName == explicitContainer }) {
                            trace.report(ErrorsJvm.REPEATED_ANNOTATION_WITH_CONTAINER.on(entry, fqName, explicitContainer))
                        }
                    }
                    else -> {
                        trace.report(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION.on(entry))
                    }
                }
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }
    }

    private fun isRepeatableAnnotation(classDescriptor: ClassDescriptor): Boolean =
        classDescriptor.isAnnotatedWithKotlinRepeatable() || platformAnnotationFeaturesSupport.isRepeatableAnnotationClass(classDescriptor)

    private data class ResolvedAnnotation(
        val entry: KtAnnotationEntry,
        val descriptor: AnnotationDescriptor,
        val useSiteTarget: AnnotationUseSiteTarget?,
    )

    // For a repeatable annotation class, returns FQ name of the container annotation if it can be resolved in Kotlin.
    // This only exists when the annotation class (whether declared in Java or Kotlin) is annotated with java.lang.annotation.Repeatable,
    // in which case the container annotation is @j.l.a.Repeatable's only argument.
    private fun resolveContainerAnnotation(annotationClass: ClassDescriptor): FqName? {
        val javaRepeatable = annotationClass.annotations.findAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION) ?: return null
        val value = javaRepeatable.allValueArguments[Name.identifier("value")] as? KClassValue ?: return null
        // Local annotations are supported neither in Java nor in Kotlin.
        val normalClass = value.value as? KClassValue.Value.NormalClass ?: return null
        return normalClass.classId.asSingleFqName()
    }
}
