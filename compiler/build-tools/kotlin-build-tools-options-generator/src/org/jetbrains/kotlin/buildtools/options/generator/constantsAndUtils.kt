/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel

internal const val IMPL_PACKAGE = "org.jetbrains.kotlin.buildtools.internal.v2"
internal const val API_PACKAGE = "org.jetbrains.kotlin.buildtools.api.v2"
internal val ANNOTATION_EXPERIMENTAL = ClassName(API_PACKAGE, "ExperimentalCompilerArgument")

internal fun KotlinCompilerArgument.extractName(): String = name.uppercase().replace("-", "_").let {
    when {
        it.startsWith("XX") && it != "XX" -> it.replaceFirst("XX", "XX_")
        it.startsWith("X") && it != "X" -> it.replaceFirst("X", "X_")
        else -> it
    }
}