/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import java.io.File

object TransformersFunctions {
    @JvmStatic
    fun replaceOptionalJvmInlineAnnotationWithReal(source: String): String {
        return source.replace("OPTIONAL_JVM_INLINE_ANNOTATION", "@JvmInline")
    }

    @JvmStatic
    fun removeOptionalJvmInlineAnnotation(source: String): String {
        return source.replace("OPTIONAL_JVM_INLINE_ANNOTATION", "")
    }

    object Android {
        val forAll: List<(String) -> String> = listOf(
            TransformersFunctions::replaceOptionalJvmInlineAnnotationWithReal,
        )
        val forSpecificFile: Map<File, (String) -> String> = mapOf(
        )
    }
}