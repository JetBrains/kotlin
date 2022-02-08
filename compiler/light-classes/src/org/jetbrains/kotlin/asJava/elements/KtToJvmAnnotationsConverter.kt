/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSimpleAnnotation
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSupport
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

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

private val targetMappings = EnumMap<JvmTarget, Map<String, EnumValue>>(JvmTarget::class.java).also { result ->
    val javaAnnotationElementTypeId = ClassId.fromString(JvmAnnotationNames.ELEMENT_TYPE_ENUM.asString())
    val jdk6 = hashMapOf(
        "kotlin.annotation.AnnotationTarget.CLASS" to EnumValue(javaAnnotationElementTypeId, Name.identifier("TYPE")),
        "kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS" to EnumValue(javaAnnotationElementTypeId, Name.identifier("ANNOTATION_TYPE")),
        "kotlin.annotation.AnnotationTarget.FIELD" to EnumValue(javaAnnotationElementTypeId, Name.identifier("FIELD")),
        "kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE" to EnumValue(javaAnnotationElementTypeId, Name.identifier("LOCAL_VARIABLE")),
        "kotlin.annotation.AnnotationTarget.VALUE_PARAMETER" to EnumValue(javaAnnotationElementTypeId, Name.identifier("PARAMETER")),
        "kotlin.annotation.AnnotationTarget.CONSTRUCTOR" to EnumValue(javaAnnotationElementTypeId, Name.identifier("CONSTRUCTOR")),
        "kotlin.annotation.AnnotationTarget.FUNCTION" to EnumValue(javaAnnotationElementTypeId, Name.identifier("METHOD")),
        "kotlin.annotation.AnnotationTarget.PROPERTY_GETTER" to EnumValue(javaAnnotationElementTypeId, Name.identifier("METHOD")),
        "kotlin.annotation.AnnotationTarget.PROPERTY_SETTER" to EnumValue(javaAnnotationElementTypeId, Name.identifier("METHOD"))
    )
    val jdk8AndLater = HashMap(jdk6).apply {
        put("kotlin.annotation.AnnotationTarget.TYPE_PARAMETER", EnumValue(javaAnnotationElementTypeId, Name.identifier("TYPE_PARAMETER")))
        put("kotlin.annotation.AnnotationTarget.TYPE", EnumValue(javaAnnotationElementTypeId, Name.identifier("TYPE_USE")))
    }
    for (target in JvmTarget.values()) {
        result[target] = if (target >= JvmTarget.JVM_1_8) jdk8AndLater else jdk6
    }
}

internal fun PsiAnnotation.tryConvertAsTarget(support: KtUltraLightSupport): KtLightAbstractAnnotation? {

    if (FqNames.target.asString() != qualifiedName) return null

    val attributeValues = extractArrayAnnotationFqNames("allowedTargets")
        ?: extractAnnotationFqName("value")?.let { listOf(it) }

    attributeValues ?: return null

    val targetMapping = targetMappings.getValue(support.typeMapper.jvmTarget)
    val convertedValues = attributeValues.mapNotNull { targetMapping[it] }.distinct()

    val targetAttributes = "value" to ArrayValue(convertedValues) { module -> module.builtIns.array.defaultType }

    return KtUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_TARGET,
        listOf(targetAttributes),
        support,
        parent
    )
}

private val javaAnnotationRetentionPolicyId = ClassId.fromString(JvmAnnotationNames.RETENTION_POLICY_ENUM.asString())
private val retentionMapping = hashMapOf(
    "kotlin.annotation.AnnotationRetention.SOURCE" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("SOURCE")),
    "kotlin.annotation.AnnotationRetention.BINARY" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("CLASS")),
    "kotlin.annotation.AnnotationRetention.RUNTIME" to EnumValue(javaAnnotationRetentionPolicyId, Name.identifier("RUNTIME"))
)


internal fun createRetentionRuntimeAnnotation(support: KtUltraLightSupport, parent: PsiElement) = KtUltraLightSimpleAnnotation(
    JAVA_LANG_ANNOTATION_RETENTION,
    listOf("value" to retentionMapping["kotlin.annotation.AnnotationRetention.RUNTIME"]!!),
    support,
    parent
)

internal fun PsiAnnotation.tryConvertAsRetention(support: KtUltraLightSupport): KtLightAbstractAnnotation? {
    if (FqNames.retention.asString() != qualifiedName) return null
    val convertedValue = extractAnnotationFqName("value")?.let { retentionMapping[it] } ?: return null
    return KtUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_RETENTION,
        listOf("value" to convertedValue),
        support,
        parent
    )
}

internal fun PsiAnnotation.tryConvertAsMustBeDocumented(support: KtUltraLightSupport): KtLightAbstractAnnotation? = tryConvertAs(
    support,
    FqNames.mustBeDocumented,
    JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
)

internal fun PsiAnnotation.tryConvertAsRepeatable(
    support: KtUltraLightSupport,
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>,
): KtLightAbstractAnnotation? {
    if (FqNames.repeatable.asString() != qualifiedName) return null
    val value = owner.kotlinOrigin
        ?.safeAs<KtClass>()
        ?.getClassId()
        ?.createNestedClassId(Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME))
        ?.let { "value" to KClassValue(it, 0) }

    return KtUltraLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_REPEATABLE,
        listOfNotNull(value),
        support,
        parent,
    ) {
        safeAs<KtLightAnnotationForSourceEntry>()?.kotlinOrigin?.safeAs<KtAnnotationEntry>()?.let {
            KtLightPsiJavaCodeReferenceElement(
                ktElement = it,
                reference = { null },
                clsDelegateProvider = { null },
                customReferenceName = JAVA_LANG_ANNOTATION_REPEATABLE_SHORT_NAME,
            )
        }
    }
}

internal val JAVA_LANG_ANNOTATION_REPEATABLE_SHORT_NAME: String get() = FqNames.repeatable.shortName().asString()

internal fun PsiAnnotation.tryConvertAsRepeatableContainer(support: KtUltraLightSupport): KtLightAbstractAnnotation? = tryConvertAs(
    support,
    FqNames.repeatable,
    KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER,
)

private fun PsiAnnotation.tryConvertAs(
    support: KtUltraLightSupport,
    from: FqName,
    to: String,
): KtLightAbstractAnnotation? = takeIf { from.asString() == qualifiedName }?.let {
    KtUltraLightSimpleAnnotation(to, emptyList(), support, parent)
}
