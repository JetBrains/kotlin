/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

internal object AbstractClassAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement
    ) {
        if (!owner.parent.isAnnotationClass()) return

        addAllAnnotationsFromAnnotationClass(currentRawAnnotations, foundQualifiers, owner)
    }

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = if (owner.parent.isAnnotationClass())
        findAdditionalAnnotationFromAnnotationClass(annotationsBox, qualifiedName, owner)
    else
        null

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

private fun PsiElement.isAnnotationClass(): Boolean = this is PsiClass && isAnnotationType

private fun addAllAnnotationsFromAnnotationClass(
    currentRawAnnotations: MutableList<in PsiAnnotation>,
    foundQualifiers: MutableSet<String>,
    owner: PsiElement,
) {
    for (index in currentRawAnnotations.indices) {
        val currentAnnotation = currentRawAnnotations[index] as? SymbolLightLazyAnnotation ?: continue
        val newAnnotation = currentAnnotation.tryConvertToRetentionJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToTargetJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToDocumentedJavaAnnotation(owner)
            ?: currentAnnotation.tryConvertToRepeatableJavaAnnotation(owner)
            ?: continue

        val qualifiedName = newAnnotation.qualifiedName
        requireNotNull(qualifiedName) { "The annotation must have 'qualifiedName'" }

        if (!foundQualifiers.add(qualifiedName)) continue
        currentRawAnnotations += newAnnotation
    }

    if (foundQualifiers.add(JvmAnnotationNames.RETENTION_ANNOTATION.asString())) {
        currentRawAnnotations += createRetentionJavaAnnotation(owner)
    }
}

private fun findAdditionalAnnotationFromAnnotationClass(
    annotationsBox: GranularAnnotationsBox,
    qualifiedName: String,
    owner: PsiElement,
): PsiAnnotation? = annotationsBox.tryConvertToRetentionJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToTargetJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToDocumentedJavaAnnotation(qualifiedName, owner)
    ?: annotationsBox.tryConvertToRepeatableJavaAnnotation(qualifiedName, owner)


private fun GranularAnnotationsBox.tryConvertToDocumentedJavaAnnotation(
    qualifiedName: String,
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.mustBeDocumented.asString(),
    owner = owner,
)

private fun SymbolLightLazyAnnotation.tryConvertToDocumentedJavaAnnotation(
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.mustBeDocumented.asString(),
    owner = owner,
)

private fun GranularAnnotationsBox.tryConvertToRetentionJavaAnnotation(
    qualifiedName: String,
    owner: PsiElement,
): PsiAnnotation? {
    val javaQualifier = JvmAnnotationNames.RETENTION_ANNOTATION.asString()
    return tryConvertToJavaAnnotation(
        qualifiedName = qualifiedName,
        javaQualifier = javaQualifier,
        kotlinQualifier = StandardNames.FqNames.retention.asString(),
        owner = owner,
        argumentsComputer = SymbolLightJavaAnnotation::computeJavaRetentionArguments,
    ) ?: owner.takeIf { qualifiedName == javaQualifier }?.let(::createRetentionJavaAnnotation)
}

private fun SymbolLightLazyAnnotation.tryConvertToRetentionJavaAnnotation(
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.retention.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeJavaRetentionArguments,
)

private fun SymbolLightJavaAnnotation.computeJavaRetentionArguments(): List<AnnotationArgument> {
    val argumentWithKotlinRetention = originalLightAnnotation.annotationApplicationWithArgumentsInfo
        .value
        .annotation
        .arguments
        .firstOrNull {
            it.name == StandardNames.DEFAULT_VALUE_PARAMETER
        }?.value as? AnnotationValue.EnumValue

    val kotlinRetentionName = argumentWithKotlinRetention?.callableId?.callableName?.asString()
    return javaRetentionArguments(kotlinRetentionName)
}

private fun createRetentionJavaAnnotation(owner: PsiElement): PsiAnnotation = SymbolLightSimpleAnnotation(
    fqName = JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    parent = owner,
    arguments = javaRetentionArguments(kotlinRetentionName = null),
)

private fun javaRetentionArguments(kotlinRetentionName: String?): List<AnnotationArgument> = listOf(
    AnnotationArgument(
        name = StandardNames.DEFAULT_VALUE_PARAMETER,
        value = AnnotationValue.EnumValue(
            callableId = CallableId(
                JvmStandardClassIds.Annotations.Java.RetentionPolicy,
                Name.identifier(retentionMapping(kotlinRetentionName ?: AnnotationRetention.RUNTIME.name)),
            ),
            sourcePsi = null,
        )
    )
)

private fun retentionMapping(name: String): String = when (name) {
    AnnotationRetention.BINARY.name -> RetentionPolicy.CLASS.name
    else -> name
}

private fun GranularAnnotationsBox.tryConvertToRepeatableJavaAnnotation(
    qualifiedName: String,
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.repeatable.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeRepeatableJavaAnnotationArguments
)

private fun SymbolLightLazyAnnotation.tryConvertToRepeatableJavaAnnotation(
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.repeatable.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeRepeatableJavaAnnotationArguments,
)

private fun SymbolLightJavaAnnotation.computeRepeatableJavaAnnotationArguments(): List<AnnotationArgument> {
    val annotationClassId = originalLightAnnotation.annotationsProvider.ownerClassId() ?: return emptyList()

    return listOf(
        AnnotationArgument(
            name = StandardNames.DEFAULT_VALUE_PARAMETER,
            value = AnnotationValue.KClass(
                classId = annotationClassId.createNestedClassId(Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)),
                isError = false,
                sourcePsi = null,
            )
        )
    )
}

private fun GranularAnnotationsBox.tryConvertToTargetJavaAnnotation(
    qualifiedName: String,
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    qualifiedName = qualifiedName,
    javaQualifier = JvmAnnotationNames.TARGET_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.target.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeTargetJavaAnnotationArguments,
)

private fun SymbolLightLazyAnnotation.tryConvertToTargetJavaAnnotation(
    owner: PsiElement,
): PsiAnnotation? = tryConvertToJavaAnnotation(
    javaQualifier = JvmAnnotationNames.TARGET_ANNOTATION.asString(),
    kotlinQualifier = StandardNames.FqNames.target.asString(),
    owner = owner,
    argumentsComputer = SymbolLightJavaAnnotation::computeTargetJavaAnnotationArguments,
)

private fun SymbolLightJavaAnnotation.computeTargetJavaAnnotationArguments(): List<AnnotationArgument> {
    val allowedKotlinTargets = originalLightAnnotation.annotationApplicationWithArgumentsInfo
        .value
        .annotation
        .arguments
        .firstOrNull()
        ?.value as? AnnotationValue.Array
        ?: return emptyList()

    val javaTargetNames = allowedKotlinTargets.values.mapNotNullTo(linkedSetOf(), AnnotationValue::mapToJavaTarget)
    return listOf(
        AnnotationArgument(
            name = StandardNames.DEFAULT_VALUE_PARAMETER,
            value = AnnotationValue.Array(
                values = javaTargetNames.map {
                    AnnotationValue.EnumValue(
                        callableId = CallableId(
                            classId = JvmStandardClassIds.Annotations.Java.ElementType,
                            callableName = Name.identifier(it),
                        ),
                        sourcePsi = null,
                    )
                },
                sourcePsi = null,
            )
        )
    )
}

private fun AnnotationValue.mapToJavaTarget(): String? {
    if (this !is AnnotationValue.EnumValue) return null

    val callableId = callableId ?: return null
    if (callableId.classId != StandardClassIds.AnnotationTarget) return null
    return when (callableId.callableName.asString()) {
        AnnotationTarget.CLASS.name -> ElementType.TYPE
        AnnotationTarget.ANNOTATION_CLASS.name -> ElementType.ANNOTATION_TYPE
        AnnotationTarget.FIELD.name -> ElementType.FIELD
        AnnotationTarget.LOCAL_VARIABLE.name -> ElementType.LOCAL_VARIABLE
        AnnotationTarget.VALUE_PARAMETER.name -> ElementType.PARAMETER
        AnnotationTarget.CONSTRUCTOR.name -> ElementType.CONSTRUCTOR
        AnnotationTarget.FUNCTION.name, AnnotationTarget.PROPERTY_GETTER.name, AnnotationTarget.PROPERTY_SETTER.name -> ElementType.METHOD
        AnnotationTarget.TYPE_PARAMETER.name -> ElementType.TYPE_PARAMETER
        AnnotationTarget.TYPE.name -> ElementType.TYPE_USE
        else -> null
    }?.name
}

private fun GranularAnnotationsBox.tryConvertToJavaAnnotation(
    qualifiedName: String,
    javaQualifier: String,
    kotlinQualifier: String,
    owner: PsiElement,
    argumentsComputer: SymbolLightJavaAnnotation.() -> List<AnnotationArgument> = { emptyList() },
): PsiAnnotation? {
    if (qualifiedName != javaQualifier) return null
    if (hasAnnotation(owner, javaQualifier)) return null

    val originalLightAnnotation = findAnnotation(
        owner,
        kotlinQualifier,
        withAdditionalAnnotations = false,
    ) as? SymbolLightLazyAnnotation ?: return null

    return SymbolLightJavaAnnotation(
        originalLightAnnotation = originalLightAnnotation,
        javaQualifier = javaQualifier,
        argumentsComputer = argumentsComputer,
        owner = owner,
    )
}

private fun SymbolLightLazyAnnotation.tryConvertToJavaAnnotation(
    javaQualifier: String,
    kotlinQualifier: String,
    owner: PsiElement,
    argumentsComputer: SymbolLightJavaAnnotation.() -> List<AnnotationArgument> = { emptyList() },
): PsiAnnotation? {
    if (qualifiedName != kotlinQualifier) return null
    return SymbolLightJavaAnnotation(
        originalLightAnnotation = this,
        javaQualifier = javaQualifier,
        argumentsComputer = argumentsComputer,
        owner = owner,
    )
}
