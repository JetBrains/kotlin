/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.utils.atMostOne

fun AnnotationDescriptor.argumentValue(parameterName: String): ConstantValue<*>? {
    return allValueArguments[Name.identifier(parameterName)].takeUnless { it is ErrorValue }
}

fun AnnotationDescriptor.getAnnotationStringValue(name: String): String {
    return (argumentValue(name) as? StringValue)?.value ?: error("Expected value $name at annotation $this")
}

inline fun <reified T> AnnotationDescriptor.getArgumentValueOrNull(name: String): T? {
    val constantValue = this.allValueArguments.entries.atMostOne {
        it.key.asString() == name
    }?.value
    return constantValue?.value as T?
}