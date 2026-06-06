/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import kotlin.metadata.*
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.signature

internal fun KmFunction.mapSignature(container: KmClass?): JvmMethodSignature =
    mapSignature(name, typeParameters, contextParameters, receiverParameterType, valueParameters, returnType, container)

/**
 * Computes the JVM signature of a function/constructor loaded from metadata.
 *
 * Note that this is only required for declarations loaded from _builtins_ metadata (`.kotlin_builtins` files). For metadata loaded from
 * JVM class files, signature is already present in the [KmFunction.signature] attribute.
 */
private fun mapSignature(
    name: String,
    typeParameters: List<KmTypeParameter>,
    contextParameters: List<KmValueParameter>,
    receiverParameterType: KmType?,
    valueParameters: List<KmValueParameter>,
    returnType: KmType,
    container: KmClass?,
): JvmMethodSignature {
    val allTypeParameters = typeParameters.associateByTo(mutableMapOf()) { it.id }
    container?.typeParameters?.associateByTo(allTypeParameters) { it.id }
    val c = ReflectTypeMappingContext(allTypeParameters) { "`$name` in ${container?.name}" }
    val desc = buildString {
        append("(")
        contextParameters.forEach { mapType(it.type, c) }
        receiverParameterType?.let { mapType(it, c) }
        valueParameters.forEach { mapType(it.type, c) }
        append(")")
        if (returnType.isNonNullUnit) append("V") else mapType(returnType, c)
    }
    return JvmMethodSignature(name, desc)
}

private class ReflectTypeMappingContext(
    val typeParameters: Map<Int, KmTypeParameter>,
    val memberNameForDebug: () -> String,
)

private val KmType.isNonNullUnit: Boolean
    get() = (classifier as? KmClassifier.Class)?.name == "kotlin/Unit" && !isNullable

private fun StringBuilder.mapType(type: KmType, c: ReflectTypeMappingContext, wrapPrimitives: Boolean = false): StringBuilder {
    return when (val classifier = type.classifier) {
        is KmClassifier.Class -> if (classifier.name == "kotlin/Array") {
            append("[")
            val (variance, type) = type.arguments.single()
            if (variance == KmVariance.IN || type == null) {
                append("Ljava/lang/Object;")
            } else {
                mapType(type, c, wrapPrimitives = true)
            }
        } else {
            mapClass(classifier.name, wrapPrimitives || type.isNullable)
        }
        is KmClassifier.TypeParameter -> {
            val typeParameter = c.typeParameters[classifier.id] ?: throw KotlinReflectionInternalError(
                "Unknown type parameter ${classifier.id} when computing signature for ${c.memberNameForDebug()}"
            )
            // Note that in the general case, using first upper bound is incorrect, and representative upper bound should be used instead
            // (first upper bound whose classifier is a non-interface non-annotation class if it exists, or just the first upper bound
            // otherwise). Here we rely on the fact that builtins do not have such signatures. If such signatures appear, this code needs
            // to be adapted.
            val upperBound = typeParameter.upperBounds.firstOrNull()
            if (upperBound != null) {
                mapType(upperBound, c)
            } else {
                append("Ljava/lang/Object;")
            }
        }
        is KmClassifier.TypeAlias ->
            throw KotlinReflectionInternalError("Type alias types cannot appear outside of abbreviation: ${c.memberNameForDebug()}")
    }
}

private fun StringBuilder.mapClass(name: ClassName, wrapPrimitives: Boolean): StringBuilder {
    if (wrapPrimitives) {
        val primitiveType = name.toClassId().takeIf { it.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME }
            ?.let { PrimitiveType.getByShortName(it.relativeClassName.asString()) }?.let(JvmPrimitiveType::get)
        if (primitiveType != null) {
            return append("L").append(primitiveType.wrapperFqName.asString().replace('.', '/')).append(";")
        }
    }
    // ClassMapperLite always maps `kotlin/Unit` to "V", which is correct only in return position.
    if (name == "kotlin/Unit") return append("Lkotlin/Unit;")
    return append(ClassMapperLite.mapClass(name))
}
