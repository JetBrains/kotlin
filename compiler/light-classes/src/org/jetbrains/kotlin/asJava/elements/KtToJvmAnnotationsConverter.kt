/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSimpleAnnotation
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.utils.KOTLIN_TO_JAVA_ANNOTATION_TARGETS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal const val KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER = "kotlin.jvm.internal.RepeatableContainer"

private fun PsiAnnotation.extractAnnotationFqName(attributeName: String): String? {
    val targetAttribute =
        attributes.firstOrNull { it.attributeName == attributeName } as? KtLightPsiNameValuePair
    targetAttribute ?: return null

    val valueTarget = (targetAttribute.value as? KtLightPsiLiteral)?.value as? Pair<*, *>
    valueTarget ?: return null

    val classId = valueTarget.first as? ClassId
    classId ?: return null
    val name = valueTarget.second as? Name
    name ?: return null

    return "${classId.asSingleFqName().asString()}.${name.identifier}"
}


private fun PsiAnnotation.extractArrayAnnotationFqNames(attributeName: String): List<String>? =
    attributes.firstOrNull { it.attributeName == attributeName }
        ?.let { it as? PsiNameValuePair }
        ?.let { (it.value as? PsiArrayInitializerMemberValue) }
        ?.let { arrayInitializer ->
            arrayInitializer.initializers.filterIsInstance<KtLightPsiLiteral>()
                .map { it.value }
                .filterIsInstance<Pair<ClassId, Name>>()
                .map { "${it.first.asSingleFqName().asString()}.${it.second.identifier}" }
        }

private val targetMapping = run {
    val javaAnnotationElementTypeId = ClassId.fromString(JvmAnnotationNames.ELEMENT_TYPE_ENUM.asString())
    KOTLIN_TO_JAVA_ANNOTATION_TARGETS.entries.associate { (key, value) ->
        "kotlin.annotation.AnnotationTarget.$key" to EnumValue(javaAnnotationElementTypeId, Name.identifier(value))
    }
}

internal fun PsiAnnotation.tryConvertAsTarget(): KtLightAbstractAnnotation? {

    if (FqNames.target.asString() != qualifiedName) return null

    val attributeValues = extractArrayAnnotationFqNames("allowedTargets")
        ?: extractAnnotationFqName("value")?.let { listOf(it) }

    attributeValues ?: return null

    val convertedValues = attributeValues.mapNotNull { targetMapping[it] }.distinct()

    val targetAttributes = "value" to ArrayValue(convertedValues) { module -> module.builtIns.array.defaultType }

    return asUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_TARGET,
        listOf(targetAttributes),
    )
}

private val javaAnnotationRetentionPolicyId = ClassId.fromString(JvmAnnotationNames.RETENTION_POLICY_ENUM.asString())
private val retentionMapping = hashMapOf(
    "kotlin.annotation.AnnotationRetention.SOURCE" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("SOURCE")),
    "kotlin.annotation.AnnotationRetention.BINARY" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("CLASS")),
    "kotlin.annotation.AnnotationRetention.RUNTIME" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("RUNTIME"))
)


internal fun createRetentionRuntimeAnnotation(parent: PsiElement) = KtUltraLightSimpleAnnotation(
    JAVA_LANG_ANNOTATION_RETENTION,
    listOf("value" to retentionMapping["kotlin.annotation.AnnotationRetention.RUNTIME"]!!),
    parent,
)

internal fun PsiAnnotation.asUltraLightSimpleAnnotation(
    qualifier: String,
    argumentsList: List<Pair<String, ConstantValue<*>>>,
): KtLightAbstractAnnotation = KtUltraLightSimpleAnnotation(
    annotationFqName = qualifier,
    argumentsList = argumentsList,
    parent = parent,
    nameReferenceElementProvider = { nameReferenceElement },
)

internal fun PsiAnnotation.tryConvertAsRetention(): KtLightAbstractAnnotation? {
    if (FqNames.retention.asString() != qualifiedName) return null
    val convertedValue = extractAnnotationFqName("value")?.let { retentionMapping[it] } ?: return null
    return asUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_RETENTION,
        listOf("value" to convertedValue),
    )
}

internal fun PsiAnnotation.tryConvertAsMustBeDocumented(): KtLightAbstractAnnotation? = tryConvertAs(
    FqNames.mustBeDocumented,
    JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
)

internal fun PsiAnnotation.tryConvertAsRepeatable(owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>): KtLightAbstractAnnotation? {
    if (FqNames.repeatable.asString() != qualifiedName) return null
    val value = owner.kotlinOrigin
        ?.safeAs<KtClass>()
        ?.getClassId()
        ?.createNestedClassId(Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME))
        ?.let { "value" to KClassValue(it, 0) }

    return KtUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_REPEATABLE,
        listOfNotNull(value),
        parent,
    ) {
        safeAs<KtLightAnnotationForSourceEntry>()?.kotlinOrigin?.safeAs<KtAnnotationEntry>()?.let {
            KtLightPsiJavaCodeReferenceElement(
                ktElement = it,
                reference = { null },
                customReferenceName = JAVA_LANG_ANNOTATION_REPEATABLE_SHORT_NAME,
            )
        }
    }
}

internal val JAVA_LANG_ANNOTATION_REPEATABLE_SHORT_NAME: String get() = FqNames.repeatable.shortName().asString()

internal fun PsiAnnotation.tryConvertAsRepeatableContainer(): KtLightAbstractAnnotation? = tryConvertAs(
    FqNames.repeatable,
    KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER,
)

private fun PsiAnnotation.tryConvertAs(from: FqName, to: String): KtLightAbstractAnnotation? =
    takeIf { from.asString() == qualifiedName }?.let {
        asUltraLightSimpleAnnotation(to, emptyList())
    }
