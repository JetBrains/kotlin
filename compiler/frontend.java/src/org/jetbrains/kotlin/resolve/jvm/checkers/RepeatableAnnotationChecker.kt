/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotatedWithKotlinRepeatable
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformAnnotationFeaturesSupport
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.TypeUtils

class RepeatableAnnotationChecker(
    private val languageVersionSettings: LanguageVersionSettings,
    private val jvmTarget: JvmTarget,
    private val platformAnnotationFeaturesSupport: JvmPlatformAnnotationFeaturesSupport,
    private val module: ModuleDescriptor,
) : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
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

        if (annotated is KtClassOrObject && annotated.hasModifier(KtTokens.ANNOTATION_KEYWORD)) {
            val annotationClass = trace.get(BindingContext.CLASS, annotated)
            if (annotationClass != null) {
                val javaRepeatable = annotations.find { it.descriptor.fqName == JvmAnnotationNames.REPEATABLE_ANNOTATION }
                val kotlinRepeatable = annotations.find { it.descriptor.fqName == StandardNames.FqNames.repeatable }
                when {
                    javaRepeatable != null -> checkJavaRepeatableAnnotationDeclaration(javaRepeatable, annotationClass, trace)
                    kotlinRepeatable != null -> checkKotlinRepeatableAnnotationDeclaration(kotlinRepeatable, annotationClass, trace)
                }
                if (javaRepeatable != null && kotlinRepeatable != null) {
                    trace.report(
                        ErrorsJvm.REDUNDANT_REPEATABLE_ANNOTATION.on(
                            kotlinRepeatable.entry,
                            kotlinRepeatable.descriptor.abbreviationFqName ?: FqName.ROOT,
                            javaRepeatable.descriptor.abbreviationFqName ?: FqName.ROOT,
                        )
                    )
                }
            }
        }
    }

    private fun checkJavaRepeatableAnnotationDeclaration(
        javaRepeatable: ResolvedAnnotation,
        annotationClass: ClassDescriptor,
        trace: BindingTrace,
    ) {
        val containerKClassValue = javaRepeatable.descriptor.allValueArguments[Name.identifier("value")]
        if (containerKClassValue is KClassValue) {
            val containerClass = TypeUtils.getClassDescriptor(containerKClassValue.getArgumentType(module))
            if (containerClass != null) {
                checkRepeatableAnnotationContainer(annotationClass, containerClass, trace, javaRepeatable.entry)
            }
        }
    }

    private fun checkKotlinRepeatableAnnotationDeclaration(
        kotlinRepeatable: ResolvedAnnotation,
        annotationClass: ClassDescriptor,
        trace: BindingTrace,
    ) {
        val nestedClassNamedContainer =
            annotationClass.unsubstitutedInnerClassesScope.getContributedClassifier(
                Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME),
                NoLookupLocation.FOR_ALREADY_TRACKED
            )
        if (nestedClassNamedContainer != null) {
            trace.report(
                ErrorsJvm.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER.on(
                    languageVersionSettings,
                    kotlinRepeatable.entry
                )
            )
        }
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
                if (languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations)) {
                    // It's not allowed to have both a repeated annotation (applied more than once) and its container
                    // on the same element. See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.5.
                    val explicitContainer = resolveContainerAnnotation(classDescriptor)
                    if (explicitContainer != null && annotations.any { it.descriptor.fqName == explicitContainer }) {
                        trace.report(ErrorsJvm.REPEATED_ANNOTATION_WITH_CONTAINER.on(entry, fqName, explicitContainer))
                    }
                } else {
                    trace.report(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION.on(entry))
                }
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }
    }

    private fun checkRepeatableAnnotationContainer(
        annotationClass: ClassDescriptor,
        containerClass: ClassDescriptor,
        trace: BindingTrace,
        reportOn: KtAnnotationEntry,
    ) {
        checkContainerParameters(containerClass, annotationClass, reportOn)?.let(trace::report)
        checkContainerRetention(containerClass, annotationClass, reportOn)?.let(trace::report)
        checkContainerTarget(containerClass, annotationClass, reportOn)?.let(trace::report)
    }

    private fun checkContainerParameters(
        containerClass: ClassDescriptor,
        annotationClass: ClassDescriptor,
        reportOn: KtAnnotationEntry,
    ): Diagnostic? {
        val containerCtor = containerClass.unsubstitutedPrimaryConstructor ?: return null

        val value = containerCtor.valueParameters.find { it.name.asString() == "value" }
        if (value == null || !KotlinBuiltIns.isArray(value.type) ||
            value.type.arguments.single().type.constructor.declarationDescriptor != annotationClass
        ) {
            return ErrorsJvm.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY
                .on(languageVersionSettings, reportOn, containerClass.fqNameSafe, annotationClass.fqNameSafe)
        }

        val otherNonDefault = containerCtor.valueParameters.find { it.name.asString() != "value" && !it.declaresDefaultValue() }
        if (otherNonDefault != null) {
            return ErrorsJvm.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER
                .on(languageVersionSettings, reportOn, containerClass.fqNameSafe, otherNonDefault)
        }

        return null
    }

    private fun checkContainerRetention(
        containerClass: ClassDescriptor,
        annotationClass: ClassDescriptor,
        reportOn: KtAnnotationEntry,
    ): Diagnostic? {
        val annotationRetention = annotationClass.getAnnotationRetention() ?: KotlinRetention.RUNTIME
        val containerRetention = containerClass.getAnnotationRetention() ?: KotlinRetention.RUNTIME
        if (containerRetention > annotationRetention) {
            return ErrorsJvm.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION
                .on(
                    languageVersionSettings,
                    reportOn,
                    containerClass.fqNameSafe,
                    containerRetention.name,
                    annotationClass.fqNameSafe,
                    annotationRetention.name
                )
        }
        return null
    }

    private fun checkContainerTarget(
        containerClass: ClassDescriptor,
        annotationClass: ClassDescriptor,
        reportOn: KtAnnotationEntry,
    ): Diagnostic? {
        val annotationTargets = AnnotationChecker.applicableTargetSet(annotationClass)
        val containerTargets = AnnotationChecker.applicableTargetSet(containerClass)

        // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.6.3.
        // (TBH, the rules about TYPE/TYPE_USE and TYPE_PARAMETER/TYPE_USE don't seem to make a lot of sense, but it's JLS
        // so we better obey it for full interop with the Java language and reflection.)
        for (target in containerTargets) {
            val ok = when (target) {
                in annotationTargets -> true
                KotlinTarget.ANNOTATION_CLASS ->
                    KotlinTarget.CLASS in annotationTargets ||
                            KotlinTarget.TYPE in annotationTargets
                KotlinTarget.CLASS ->
                    KotlinTarget.TYPE in annotationTargets
                KotlinTarget.TYPE_PARAMETER ->
                    KotlinTarget.TYPE in annotationTargets
                else -> false
            }
            if (!ok) {
                return ErrorsJvm.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET
                    .on(languageVersionSettings, reportOn, containerClass.fqNameSafe, annotationClass.fqNameSafe)
            }
        }

        return null
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
