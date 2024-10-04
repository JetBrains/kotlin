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

private fun ClassLoader.loadAnnotationClassWithMethod(fqName: String, methodName: String): Pair<Class<*>, Method> {
    val klass = loadClass(fqName)
    val method = klass.methods.find { it.name == methodName } ?: error("No `$methodName` in `@$fqName`")
    return klass to method
}

private fun Field.getAnnotationNamedAs(annotationClass: Class<*>) = annotations
    .find { it.annotationClass.qualifiedName == annotationClass.canonicalName }

private fun Field.getAnnotationsNamedAs(annotationClass: Class<*>) = annotations
    .filter { it.annotationClass.qualifiedName == annotationClass.canonicalName }

fun buildRuntimeFeatureToFlagMap(classLoader: ClassLoader): Map<LanguageFeature, String> {
    val compilerArgumentsClass = classLoader.loadClass(COMPILER_ARGUMENTS_CLASS)

    val (argumentClass, getValueFromArgument) = classLoader.loadAnnotationClassWithMethod(ARGUMENT_CLASS, ARGUMENT_VALUE)
    val (enablesClass, getFeatureFromEnables) = classLoader.loadAnnotationClassWithMethod(ENABLES_CLASS, FEATURE)
    val (disablesClass, getFeatureFromDisables) = classLoader.loadAnnotationClassWithMethod(DISABLES_CLASS, FEATURE)

    return compilerArgumentsClass.declaredFields.flatMap { field ->
        val name = field.getAnnotationNamedAs(argumentClass)?.let { getValueFromArgument(it) as? String }
            ?: return@flatMap emptyList()
        val features = field.getAnnotationsNamedAs(enablesClass).map { getFeatureFromEnables(it) as LanguageFeature } +
                field.getAnnotationsNamedAs(disablesClass).map { getFeatureFromDisables(it) as LanguageFeature }
        features.map { it to name }
    }.toMap()
}
