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
import org.jetbrains.kotlin.load.java.lazy.NullabilityQualifierWithApplicability
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.ReportLevel
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

private val TYPE_QUALIFIER_NICKNAME_FQNAME = FqName("javax.annotation.meta.TypeQualifierNickname")
private val TYPE_QUALIFIER_FQNAME = FqName("javax.annotation.meta.TypeQualifier")
private val TYPE_QUALIFIER_DEFAULT_FQNAME = FqName("javax.annotation.meta.TypeQualifierDefault")

private val MIGRATION_ANNOTATION_FQNAME = FqName("kotlin.annotations.jvm.UnderMigration")

private val BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS = mapOf(
        FqName("javax.annotation.ParametersAreNullableByDefault") to
                NullabilityQualifierWithApplicability(
                        NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE),
                        listOf(AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER)
                ),
        FqName("javax.annotation.ParametersAreNonnullByDefault") to
                NullabilityQualifierWithApplicability(
                        NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                        listOf(AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER)
                )
)

class AnnotationTypeQualifierResolver(storageManager: StorageManager, private val jsr305State: Jsr305State) {
    enum class QualifierApplicabilityType {
        METHOD_RETURN_TYPE, VALUE_PARAMETER, FIELD, TYPE_USE
    }

    class TypeQualifierWithApplicability(
            private val typeQualifier: AnnotationDescriptor,
            private val applicability: Int
    ) {
        operator fun component1() = typeQualifier
        operator fun component2() = QualifierApplicabilityType.values().filter(this::isApplicableTo)

        private fun isApplicableTo(elementType: QualifierApplicabilityType) =
                isApplicableConsideringMask(QualifierApplicabilityType.TYPE_USE) || isApplicableConsideringMask(elementType)

        private fun isApplicableConsideringMask(elementType: QualifierApplicabilityType) =
                (applicability and (1 shl elementType.ordinal)) != 0
    }

    private val resolvedNicknames =
            storageManager.createMemoizedFunctionWithNullableValues(this::computeTypeQualifierNickname)

    private fun computeTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (!classDescriptor.annotations.hasAnnotation(TYPE_QUALIFIER_NICKNAME_FQNAME)) return null

        return classDescriptor.annotations.firstNotNullResult(this::resolveTypeQualifierAnnotation)
    }

    private fun resolveTypeQualifierNickname(classDescriptor: ClassDescriptor): AnnotationDescriptor? {
        if (classDescriptor.kind != ClassKind.ANNOTATION_CLASS) return null

        return resolvedNicknames(classDescriptor)
    }

    fun resolveTypeQualifierAnnotation(annotationDescriptor: AnnotationDescriptor): AnnotationDescriptor? {
        if (jsr305State.disabled) {
            return null
        }

        val annotationClass = annotationDescriptor.annotationClass ?: return null
        if (annotationClass.isAnnotatedWithTypeQualifier) return annotationDescriptor

        return resolveTypeQualifierNickname(annotationClass)
    }

    fun resolveQualifierBuiltInDefaultAnnotation(annotationDescriptor: AnnotationDescriptor): NullabilityQualifierWithApplicability? {
        if (jsr305State.disabled) {
            return null
        }

        return BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS[annotationDescriptor.fqName]?.let { (qualifier, applicability) ->
            val state = resolveJsr305AnnotationState(annotationDescriptor).takeIf { it != ReportLevel.IGNORE } ?: return null
            return NullabilityQualifierWithApplicability(qualifier.copy(isForWarningOnly = state.isWarning), applicability)
        }
    }

    fun resolveTypeQualifierDefaultAnnotation(annotationDescriptor: AnnotationDescriptor): TypeQualifierWithApplicability? {
        if (jsr305State.disabled) {
            return null
        }

        val typeQualifierDefaultAnnotatedClass =
                annotationDescriptor.annotationClass?.takeIf { it.annotations.hasAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME) }
                ?: return null

        val elementTypesMask =
                annotationDescriptor.annotationClass!!
                        .annotations.findAnnotation(TYPE_QUALIFIER_DEFAULT_FQNAME)!!
                        .allValueArguments
                        .flatMap { (parameter, argument) ->
                            if (parameter == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME)
                                argument.mapConstantToQualifierApplicabilityTypes()
                            else
                                emptyList()
                        }
                        .fold(0) { acc: Int, applicabilityType -> acc or (1 shl applicabilityType.ordinal) }

        val typeQualifier = typeQualifierDefaultAnnotatedClass.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null }
                            ?: return null

        return TypeQualifierWithApplicability(typeQualifier, elementTypesMask)
    }

    fun resolveJsr305AnnotationState(annotationDescriptor: AnnotationDescriptor): ReportLevel {
        resolveJsr305CustomState(annotationDescriptor)?.let { return it }
        return jsr305State.global
    }

    fun resolveJsr305CustomState(annotationDescriptor: AnnotationDescriptor): ReportLevel? {
        jsr305State.user[annotationDescriptor.fqName?.asString()]?.let { return it }
        return annotationDescriptor.annotationClass?.migrationAnnotationStatus()
    }

    private fun ClassDescriptor.migrationAnnotationStatus(): ReportLevel? {
        val enumValue = annotations.findAnnotation(MIGRATION_ANNOTATION_FQNAME)?.firstArgument() as? EnumValue
                        ?: return null

        jsr305State.migration?.let { return it }

        return when (enumValue.enumEntryName.asString()) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    private fun ConstantValue<*>.mapConstantToQualifierApplicabilityTypes(): List<QualifierApplicabilityType> =
        when (this) {
            is ArrayValue -> value.flatMap { it.mapConstantToQualifierApplicabilityTypes() }
            is EnumValue -> listOfNotNull(
                    when (enumEntryName.identifier) {
                        "METHOD" -> QualifierApplicabilityType.METHOD_RETURN_TYPE
                        "FIELD" -> QualifierApplicabilityType.FIELD
                        "PARAMETER" -> QualifierApplicabilityType.VALUE_PARAMETER
                        "TYPE_USE" -> QualifierApplicabilityType.TYPE_USE
                        else -> null
                    }
            )
            else -> emptyList()
        }

    val disabled: Boolean = jsr305State.disabled
}

val BUILT_IN_TYPE_QUALIFIER_FQ_NAMES = setOf(JAVAX_NONNULL_ANNOTATION, JAVAX_CHECKFORNULL_ANNOTATION)

private val ClassDescriptor.isAnnotatedWithTypeQualifier: Boolean
    get() = fqNameSafe in BUILT_IN_TYPE_QUALIFIER_FQ_NAMES || annotations.hasAnnotation(TYPE_QUALIFIER_FQNAME)
