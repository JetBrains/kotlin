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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.components.JavaAnnotationTargetMapper
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.StorageManager

class AnnotationTypeQualifierResolver(storageManager: StorageManager, private val javaTypeEnhancementState: JavaTypeEnhancementState) {
    class TypeQualifierWithApplicability(
        private val typeQualifier: AnnotationDescriptor,
        private val applicability: Int
    ) {
        operator fun component1() = typeQualifier
        operator fun component2() = AnnotationQualifierApplicabilityType.values().filter(this::isApplicableTo)

        private fun isApplicableTo(elementType: AnnotationQualifierApplicabilityType): Boolean {
            if (isApplicableConsideringMask(elementType)) return true

            // We explicitly state that while JSR-305 TYPE_USE annotations effectively should be applied to every type
            // they are not applicable for type parameter bounds because it would be a breaking change otherwise.
            // Only defaulting annotations from jspecify are applicable
            return isApplicableConsideringMask(AnnotationQualifierApplicabilityType.TYPE_USE) &&
                    elementType != AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
        }

        private fun isApplicableConsideringMask(elementType: AnnotationQualifierApplicabilityType) =
            (applicability and (1 shl elementType.ordinal)) != 0
    }

    private val resolvedNicknames =
        storageManager.createMemoizedFunctionWithNullableValues(this::computeTypeQualifierNickname)

    private fun computeTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (!classDescriptor.annotations.hasAnnotation(TYPE_QUALIFIER_NICKNAME_FQNAME)) return null

        return classDescriptor.annotations.firstNotNullOfOrNull(this::resolveTypeQualifierAnnotation)
    }

    private fun resolveTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) return null

        return resolvedNicknames(classDescriptor)
    }

    fun resolveTypeQualifierAnnotation(annotation: AnnotationDescriptor): AnnotationDescriptor? {
        if (javaTypeEnhancementState.jsr305.isDisabled) {
            return null
        }

        val annotationClass = annotation.annotationClass ?: return null
        if (annotationClass.isAnnotatedWithTypeQualifier) return annotation

        return resolveTypeQualifierNickname(annotationClass)
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

    fun resolveTypeQualifierDefaultAnnotation(annotation: AnnotationDescriptor): TypeQualifierWithApplicability? {
        if (javaTypeEnhancementState.jsr305.isDisabled) {
            return null
        }

        val typeQualifierDefaultAnnotatedClass =
            annotation.annotationClass?.takeIf { it.annotations.hasAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME) }
                ?: return null

        val elementTypesMask =
            annotation.annotationClass!!
                .annotations.findAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME)!!
                .allValueArguments
                .flatMap { (parameter, argument) ->
                    if (parameter == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME)
                        argument.mapJavaConstantToQualifierApplicabilityTypes()
                    else
                        emptyList()
                }
                .fold(0) { acc: Int, applicabilityType -> acc or (1 shl applicabilityType.ordinal) }

        val typeQualifier = typeQualifierDefaultAnnotatedClass.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null }
            ?: return null

        return TypeQualifierWithApplicability(typeQualifier, elementTypesMask)
    }

    fun resolveAnnotation(annotation: AnnotationDescriptor): TypeQualifierWithApplicability? {
        val annotatedClass = annotation.annotationClass ?: return null
        val target = annotatedClass.annotations.findAnnotation(JvmAnnotationNames.TARGET_ANNOTATION) ?: return null
        val elementTypesMask = target.allValueArguments
            .flatMap { (_, argument) -> argument.mapKotlinConstantToQualifierApplicabilityTypes() }
            .fold(0) { acc: Int, applicabilityType -> acc or (1 shl applicabilityType.ordinal) }

        return TypeQualifierWithApplicability(annotation, elementTypesMask)
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
        val enumValue = annotations.findAnnotation(MIGRATION_ANNOTATION_FQNAME)?.firstArgument() as? EnumValue
            ?: return null

        javaTypeEnhancementState.jsr305.migrationLevel?.let { return it }

        return when (enumValue.enumEntryName.asString()) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    private fun String.toKotlinTargetNames() = JavaAnnotationTargetMapper.mapJavaTargetArgumentByName(this).map { it.name }

    private fun ConstantValue<*>.mapConstantToQualifierApplicabilityTypes(
        findPredicate: EnumValue.(AnnotationQualifierApplicabilityType) -> Boolean
    ): List<AnnotationQualifierApplicabilityType> =
        when (this) {
            is ArrayValue -> value.flatMap { it.mapConstantToQualifierApplicabilityTypes(findPredicate) }
            is EnumValue -> listOfNotNull(AnnotationQualifierApplicabilityType.values().find { findPredicate(it) })
            else -> emptyList()
        }

    private fun ConstantValue<*>.mapJavaConstantToQualifierApplicabilityTypes(): List<AnnotationQualifierApplicabilityType> =
        mapConstantToQualifierApplicabilityTypes { enumEntryName.identifier == it.javaTarget }

    private fun ConstantValue<*>.mapKotlinConstantToQualifierApplicabilityTypes(): List<AnnotationQualifierApplicabilityType> =
        mapConstantToQualifierApplicabilityTypes { enumEntryName.identifier in it.javaTarget.toKotlinTargetNames() }
}

private val ClassDescriptor.isAnnotatedWithTypeQualifier: Boolean
    get() = fqNameSafe in BUILT_IN_TYPE_QUALIFIER_FQ_NAMES || annotations.hasAnnotation(TYPE_QUALIFIER_FQNAME)
