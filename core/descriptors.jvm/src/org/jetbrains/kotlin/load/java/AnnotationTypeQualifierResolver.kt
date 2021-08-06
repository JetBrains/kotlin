/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.components.JavaAnnotationTargetMapper
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.storage.StorageManager

typealias TypeQualifierWithApplicability = Pair<AnnotationDescriptor, Set<AnnotationQualifierApplicabilityType>>

class AnnotationTypeQualifierResolver(storageManager: StorageManager, private val javaTypeEnhancementState: JavaTypeEnhancementState) {
    private val resolvedNicknames =
        storageManager.createMemoizedFunctionWithNullableValues { klass: ClassDescriptor ->
            if (klass.annotations.hasAnnotation(TYPE_QUALIFIER_NICKNAME_FQNAME))
                klass.annotations.firstNotNullOfOrNull(this::resolveTypeQualifierAnnotation)
            else
                null
        }

    fun resolveTypeQualifierAnnotation(annotation: AnnotationDescriptor): AnnotationDescriptor? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        val annotationClass = annotation.annotationClass ?: return null
        if (annotation.fqName in BUILT_IN_TYPE_QUALIFIER_FQ_NAMES || annotationClass.annotations.hasAnnotation(TYPE_QUALIFIER_FQNAME))
            return annotation
        return resolvedNicknames(annotationClass)
    }

    fun resolveQualifierBuiltInDefaultAnnotation(annotation: AnnotationDescriptor): JavaDefaultQualifiers? {
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

    private fun resolveDefaultAnnotationState(annotation: AnnotationDescriptor): ReportLevel {
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

    fun resolveTypeQualifierDefaultAnnotation(annotation: AnnotationDescriptor): TypeQualifierWithApplicability? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null
        val annotationClass = annotation.annotationClass ?: return null
        val typeQualifierDefault = annotationClass.annotations.findAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME) ?: return null
        val typeQualifier = annotationClass.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null } ?: return null
        val applicability = typeQualifierDefault.enumArguments(onlyValue = true)
            .mapNotNullTo(mutableSetOf()) { JAVA_APPLICABILITY_TYPES[it] }
        return TypeQualifierWithApplicability(typeQualifier, applicability.allIfTypeUse())
    }

    fun isTypeUseAnnotation(annotation: AnnotationDescriptor): Boolean {
        val annotatedClass = annotation.annotationClass ?: return false
        val target = annotatedClass.annotations.findAnnotation(JvmAnnotationNames.TARGET_ANNOTATION) ?: return false
        return target.enumArguments(onlyValue = false).any { it == KotlinTarget.TYPE.name }
    }

    fun resolveJsr305AnnotationState(annotation: AnnotationDescriptor): ReportLevel {
        resolveJsr305CustomState(annotation)?.let { return it }
        return javaTypeEnhancementState.jsr305.globalLevel
    }

    fun resolveJsr305CustomState(annotation: AnnotationDescriptor): ReportLevel? {
        javaTypeEnhancementState.jsr305.userDefinedLevelForSpecificAnnotation[annotation.fqName]?.let { return it }
        return annotation.annotationClass?.migrationAnnotationStatus()
    }

    private fun ClassDescriptor.migrationAnnotationStatus(): ReportLevel? {
        val enumValue = annotations.findAnnotation(MIGRATION_ANNOTATION_FQNAME)?.enumArguments(onlyValue = false)?.firstOrNull()
            ?: return null
        return javaTypeEnhancementState.jsr305.migrationLevel ?: when (enumValue) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    private fun AnnotationDescriptor.enumArguments(onlyValue: Boolean): Iterable<String> =
        allValueArguments.flatMap { (parameter, argument) ->
            if (!onlyValue || parameter == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME)
                argument.toEnumNames()
            else
                emptyList()
        }

    private fun ConstantValue<*>.toEnumNames(): List<String> =
        when (this) {
            is ArrayValue -> value.flatMap { it.toEnumNames() }
            is EnumValue -> listOf(enumEntryName.identifier)
            else -> emptyList()
        }

    @OptIn(ExperimentalStdlibApi::class)
    private companion object {
        val JAVA_APPLICABILITY_TYPES = buildMap<String, AnnotationQualifierApplicabilityType> {
            for (type in AnnotationQualifierApplicabilityType.values()) {
                getOrPut(type.javaTarget) { type }
            }
        }

        val KOTLIN_APPLICABILITY_TYPES = buildMap<String, AnnotationQualifierApplicabilityType> {
            for (type in AnnotationQualifierApplicabilityType.values()) {
                for (target in JavaAnnotationTargetMapper.mapJavaTargetArgumentByName(type.javaTarget)) {
                    getOrPut(target.name) { type }
                }
            }
        }
    }
}
