/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.config.LanguageFeature
import java.lang.reflect.Field
import java.lang.reflect.Method

private const val COMPILER_ARGUMENTS_CLASS = "org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments"

private const val ARGUMENT_CLASS = "org.jetbrains.kotlin.cli.common.arguments.Argument"
private const val ARGUMENT_VALUE = "value"

private const val ENABLES_CLASS = "org.jetbrains.kotlin.cli.common.arguments.Enables"
private const val DISABLES_CLASS = "org.jetbrains.kotlin.cli.common.arguments.Disables"
private const val FEATURE = "feature"
private const val IF_VALUE_IS = "ifValueIs"

private fun ClassLoader.loadArgumentAnnotationInfo(): Pair<Class<out Annotation>, Method> {
    val klass = loadAnnotationClass(ARGUMENT_CLASS)
    val method = klass.getMethod(ARGUMENT_VALUE)
    return klass to method
}

private data class AnnotationAndMethods(
    val annotationClass: Class<out Annotation>,
    val featureMethod: Method,
    val ifValueIsMethod: Method,
)

private fun ClassLoader.loadEnablesOrDisablesAnnotationInfo(fqName: String): AnnotationAndMethods {
    val klass = loadAnnotationClass(fqName)
    val featureMethod = klass.getMethod(FEATURE)
    val ifValueIsMethod = klass.getMethod(IF_VALUE_IS)
    return AnnotationAndMethods(klass, featureMethod, ifValueIsMethod)
}

private fun ClassLoader.loadAnnotationClass(fqName: String): Class<out Annotation> {
    @Suppress("UNCHECKED_CAST")
    return loadClass(fqName) as Class<out Annotation>
}

private data class FeatureAndValue(val feature: LanguageFeature, val value: String)

private fun Field.getFeaturesAndValues(triple: AnnotationAndMethods): List<FeatureAndValue> {
    val (annotationClass, featureMethod, ifValueIsMethod) = triple
    return getAnnotationsByType(annotationClass).map {
        FeatureAndValue(
            feature = featureMethod(it) as LanguageFeature,
            value = ifValueIsMethod(it) as String
        )
    }
}

fun buildRuntimeFeatureToFlagMap(classLoader: ClassLoader): Map<LanguageFeature, String> {
    val compilerArgumentsClass = classLoader.loadClass(COMPILER_ARGUMENTS_CLASS)

    val (argumentClass, getValueFromArgument) = classLoader.loadArgumentAnnotationInfo()
    val enables = classLoader.loadEnablesOrDisablesAnnotationInfo(ENABLES_CLASS)
    val disables = classLoader.loadEnablesOrDisablesAnnotationInfo(DISABLES_CLASS)

    data class ArgumentAndValue(val argument: String, val value: String)

    return compilerArgumentsClass.declaredFields
        .flatMap { field ->
            val name = field.getAnnotationsByType(argumentClass).firstOrNull()?.let { getValueFromArgument(it) as? String }
                ?: return@flatMap emptyList()
            val features = field.getFeaturesAndValues(enables) + field.getFeaturesAndValues(disables)
            features.map { it to name }
        }
        .groupBy(keySelector = { it.first.feature }, valueTransform = { ArgumentAndValue(argument = it.second, value = it.first.value) })
        .mapValues { (_, values) ->
            val argument = values.first().argument
            if (values.any { it.value.isNotEmpty() }) {
                buildString {
                    append(argument)
                    append('=')
                    if (values.size == 1) {
                        append(values.first().value)
                    } else {
                        values.joinTo(buffer = this, separator = "|", prefix = "{", postfix = "}") { it.value }
                    }
                }
            } else {
                argument
            }
        }
}
