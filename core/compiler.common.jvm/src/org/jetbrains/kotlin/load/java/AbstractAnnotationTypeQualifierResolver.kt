/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap

typealias TypeQualifierWithApplicability<Annotation> = Pair<Annotation, Set<AnnotationQualifierApplicabilityType>>

abstract class AbstractAnnotationTypeQualifierResolver<Annotation : Any>(
    private val javaTypeEnhancementState: JavaTypeEnhancementState
) {
    protected abstract val Annotation.annotations: Iterable<Annotation>
    protected abstract val Annotation.key: Any
    protected abstract val Annotation.fqName: FqName?
    protected abstract fun Annotation.enumArguments(onlyValue: Boolean): Iterable<String>

    private fun Annotation.findAnnotation(fqName: FqName): Annotation? =
        annotations.find { it.fqName == fqName }

    private fun Annotation.hasAnnotation(fqName: FqName): Boolean =
        annotations.any { it.fqName == fqName }

    private val resolvedNicknames = ConcurrentHashMap<Any, Annotation>()

    fun resolveTypeQualifierAnnotation(annotation: Annotation): Annotation? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        if (annotation.fqName in BUILT_IN_TYPE_QUALIFIER_FQ_NAMES || annotation.hasAnnotation(TYPE_QUALIFIER_FQNAME))
            return annotation
        if (!annotation.hasAnnotation(TYPE_QUALIFIER_NICKNAME_FQNAME))
            return null
        return resolvedNicknames.getOrPut(annotation.key) {
            // This won't store nulls (ConcurrentHashMap does not permit that), but presumably unless the code
            // is broken a nickname should be resolvable.
            annotation.annotations.firstNotNullOfOrNull(::resolveTypeQualifierAnnotation) ?: return null
        }
    }

    fun resolveQualifierBuiltInDefaultAnnotation(annotation: Annotation): JavaDefaultQualifiers? {
        if (javaTypeEnhancementState.disabledDefaultAnnotations) {
            return null
        }

        return BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS[annotation.fqName]?.let { qualifierForDefaultingAnnotation ->
            val state = resolveDefaultAnnotationState(annotation).takeIf { it != ReportLevel.IGNORE } ?: return null
            qualifierForDefaultingAnnotation.copy(
                nullabilityQualifier = qualifierForDefaultingAnnotation.nullabilityQualifier.copy(isForWarningOnly = state.isWarning)
            )
        }
    }

    private fun resolveDefaultAnnotationState(annotation: Annotation): ReportLevel {
        val annotationFqname = annotation.fqName
        if (annotationFqname != null && annotationFqname in JSPECIFY_DEFAULT_ANNOTATIONS) {
            return javaTypeEnhancementState.getReportLevelForAnnotation(annotationFqname)
        }
        return resolveJsr305AnnotationState(annotation)
    }

    // We explicitly state that while JSR-305 TYPE_USE annotations effectively should be applied to every type.
    // They are not applicable for type parameter bounds because it would be a breaking change otherwise.
    private fun Set<AnnotationQualifierApplicabilityType>.allIfTypeUse(): Set<AnnotationQualifierApplicabilityType> =
        if (AnnotationQualifierApplicabilityType.TYPE_USE in this)
            AnnotationQualifierApplicabilityType.values().toSet() - AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS + this
        else
            this

    fun resolveTypeQualifierDefaultAnnotation(annotation: Annotation): TypeQualifierWithApplicability<Annotation>? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        val typeQualifierDefault = annotation.findAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME) ?: return null
        val typeQualifier = annotation.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null } ?: return null
        val applicability = typeQualifierDefault.enumArguments(onlyValue = true)
            .mapNotNullTo(mutableSetOf()) { JAVA_APPLICABILITY_TYPES[it] }
        return TypeQualifierWithApplicability(typeQualifier, applicability.allIfTypeUse())
    }

    fun isTypeUseAnnotation(annotation: Annotation): Boolean {
        // Expect that Java's Target was mapped to Kotlin's Target.
        val target = annotation.findAnnotation(StandardNames.FqNames.target) ?: return false
        return target.enumArguments(onlyValue = false).any { it == KotlinTarget.TYPE.name }
    }

    fun resolveJsr305AnnotationState(annotation: Annotation): ReportLevel {
        resolveJsr305CustomState(annotation)?.let { return it }
        return javaTypeEnhancementState.jsr305.globalLevel
    }

    fun resolveJsr305CustomState(annotation: Annotation): ReportLevel? {
        javaTypeEnhancementState.jsr305.userDefinedLevelForSpecificAnnotation[annotation.fqName]?.let { return it }
        val enumValue = annotation.findAnnotation(MIGRATION_ANNOTATION_FQNAME)?.enumArguments(onlyValue = false)?.firstOrNull()
            ?: return null
        return javaTypeEnhancementState.jsr305.migrationLevel ?: when (enumValue) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private companion object {
        val JAVA_APPLICABILITY_TYPES = buildMap<String, AnnotationQualifierApplicabilityType> {
            for (type in AnnotationQualifierApplicabilityType.values()) {
                getOrPut(type.javaTarget) { type }
            }
        }
    }
}
