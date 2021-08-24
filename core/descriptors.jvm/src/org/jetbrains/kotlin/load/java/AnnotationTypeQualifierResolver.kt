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

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
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

    private fun resolveQualifierBuiltInDefaultAnnotation(annotation: AnnotationDescriptor): JavaDefaultQualifiers? {
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

    private fun resolveTypeQualifierDefaultAnnotation(annotation: AnnotationDescriptor): TypeQualifierWithApplicability? {
        if (javaTypeEnhancementState.jsr305.isDisabled) return null

        val annotationClass = annotation.annotationClass ?: return null
        val typeQualifierDefault = annotationClass.annotations.findAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME) ?: return null
        val typeQualifier = annotationClass.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null } ?: return null
        val explicitApplicability = typeQualifierDefault.enumArguments(onlyValue = true)
            .mapNotNullTo(mutableSetOf()) { JAVA_APPLICABILITY_TYPES[it] }
        // We explicitly state that while JSR-305 TYPE_USE annotations effectively should be applied to every type
        // they are not applicable for type parameter bounds because it would be a breaking change otherwise.
        // Only defaulting annotations from jspecify are applicable
        val applicability = if (AnnotationQualifierApplicabilityType.TYPE_USE in explicitApplicability)
            explicitApplicability + ALL_APPLICABILITY_EXCEPT_TYPE_PARAMETER_BOUNDS
        else
            explicitApplicability
        return TypeQualifierWithApplicability(typeQualifier, applicability)
    }

    fun isTypeUseAnnotation(annotation: AnnotationDescriptor): Boolean {
        val annotatedClass = annotation.annotationClass ?: return false
        // Expect that `@Target` has been mapped to its Kotlin equivalent.
        val target = annotatedClass.annotations.findAnnotation(StandardNames.FqNames.target) ?: return false
        return target.enumArguments(onlyValue = false).any { it == KotlinTarget.TYPE.name }
    }

    private fun resolveJsr305AnnotationState(annotation: AnnotationDescriptor): ReportLevel {
        resolveJsr305CustomState(annotation)?.let { return it }
        return javaTypeEnhancementState.jsr305.globalLevel
    }

    private fun resolveJsr305CustomState(annotation: AnnotationDescriptor): ReportLevel? {
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

    private fun extractDefaultQualifiers(annotation: AnnotationDescriptor): JavaDefaultQualifiers? {
        resolveQualifierBuiltInDefaultAnnotation(annotation)?.let { return it }

        val (typeQualifier, applicability) = resolveTypeQualifierDefaultAnnotation(annotation)
            ?: return null
        val jsr305State = resolveJsr305CustomState(annotation) ?: resolveJsr305AnnotationState(typeQualifier)
        if (jsr305State.isIgnore) return null
        // TODO: since we override the warning status, whether we force it in `extractNullability` is irrelevant.
        //   However, this is probably not what was intended.
        val nullabilityQualifier = extractNullability(typeQualifier) { false } ?: return null
        return JavaDefaultQualifiers(nullabilityQualifier.copy(isForWarningOnly = jsr305State.isWarning), applicability)
    }

    fun extractAndMergeDefaultQualifiers(
        oldQualifiers: JavaTypeQualifiersByElementType?,
        annotations: Iterable<AnnotationDescriptor>
    ): JavaTypeQualifiersByElementType? {
        val defaultQualifiers = annotations.mapNotNull { extractDefaultQualifiers(it) }
        if (defaultQualifiers.isEmpty()) return oldQualifiers

        val defaultQualifiersByType =
            oldQualifiers?.defaultQualifiers?.let(::QualifierByApplicabilityType)
                ?: QualifierByApplicabilityType(AnnotationQualifierApplicabilityType::class.java)

        var wasUpdate = false
        for (qualifier in defaultQualifiers) {
            for (applicabilityType in qualifier.qualifierApplicabilityTypes) {
                defaultQualifiersByType[applicabilityType] = qualifier
                wasUpdate = true
            }
        }

        return if (!wasUpdate) oldQualifiers else JavaTypeQualifiersByElementType(defaultQualifiersByType)
    }

    private fun extractNullability(
        annotation: AnnotationDescriptor, forceWarning: AnnotationDescriptor.() -> Boolean
    ): NullabilityQualifierWithMigrationStatus? {
        knownNullability(annotation, annotation.forceWarning())?.let { return it }

        val typeQualifierAnnotation = resolveTypeQualifierAnnotation(annotation) ?: return null
        val jsr305State = resolveJsr305AnnotationState(annotation)
        if (jsr305State.isIgnore) return null
        // TODO: the result of `forceWarning` will be overwritten - expected? Probably not.
        return knownNullability(typeQualifierAnnotation, typeQualifierAnnotation.forceWarning())
            ?.copy(isForWarningOnly = jsr305State.isWarning)
    }

    fun extractNullability(
        annotations: Iterable<AnnotationDescriptor>, forceWarning: AnnotationDescriptor.() -> Boolean
    ): NullabilityQualifierWithMigrationStatus? =
        // TODO: handle inconsistent qualifiers (return null? prefer errors over warnings?)
        annotations.firstNotNullOfOrNull { extractNullability(it, forceWarning) }

    fun extractMutability(annotations: Iterable<AnnotationDescriptor>): MutabilityQualifier? {
        return annotations.fold(null as MutabilityQualifier?) { found, annotation ->
            when (annotation.fqName) {
                in READ_ONLY_ANNOTATIONS -> MutabilityQualifier.READ_ONLY
                in MUTABLE_ANNOTATIONS -> MutabilityQualifier.MUTABLE
                else -> found
            }.also { if (found != null && found != it) return null /* inconsistent */ }
        }
    }

    private fun knownNullability(annotation: AnnotationDescriptor, forceWarning: Boolean): NullabilityQualifierWithMigrationStatus? {
        val fqName = annotation.fqName ?: return null
        val reportLevel = javaTypeEnhancementState.getReportLevelForAnnotation(fqName)
        if (reportLevel.isIgnore) return null
        val nullability = when (fqName) {
            JAVAX_NONNULL_ANNOTATION ->
                when (annotation.enumArguments(onlyValue = false).firstOrNull()) {
                    "ALWAYS", null -> NullabilityQualifier.NOT_NULL
                    "MAYBE", "NEVER" -> NullabilityQualifier.NULLABLE
                    "UNKNOWN" -> NullabilityQualifier.FORCE_FLEXIBILITY
                    else -> return null
                }
            in NULLABLE_ANNOTATIONS -> NullabilityQualifier.NULLABLE
            in NOT_NULL_ANNOTATIONS -> NullabilityQualifier.NOT_NULL
            JSPECIFY_NULLNESS_UNKNOWN -> NullabilityQualifier.FORCE_FLEXIBILITY
            else -> return null
        }
        return NullabilityQualifierWithMigrationStatus(nullability, reportLevel.isWarning || forceWarning)
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

        val ALL_APPLICABILITY_EXCEPT_TYPE_PARAMETER_BOUNDS =
            AnnotationQualifierApplicabilityType.values().toSet() - AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
    }
}
