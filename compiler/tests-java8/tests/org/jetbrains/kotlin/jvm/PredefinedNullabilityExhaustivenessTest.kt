/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm

import junit.framework.TestCase
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.load.java.typeEnhancement.PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE

class PredefinedNullabilityExhaustivenessTest : TestCase() {
    fun testNullabilityDefinedForAllVisibleMappedMembers() {
        (JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES + JvmBuiltInsSignatures.VISIBLE_CONSTRUCTOR_SIGNATURES).forEach { signature ->
            assertTrue(
                "Unknown nullability for signature: $signature",
                signature in PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE || signature in EXPECTED_TO_BE_EXCLUDED_FROM_PREDEFINED_NULLABILITY
            )
        }
    }

    companion object {
        private val EXPECTED_TO_BE_EXCLUDED_FROM_PREDEFINED_NULLABILITY = setOf(
            "java/lang/CharSequence.codePoints()Ljava/util/stream/IntStream;",
            "java/lang/CharSequence.chars()Ljava/util/stream/IntStream;",
            "java/lang/Iterable.forEach(Ljava/util/function/Consumer;)V",
            "java/lang/Throwable.setStackTrace([Ljava/lang/StackTraceElement;)V",
            "java/lang/Throwable.fillInStackTrace()Ljava/lang/Throwable;",
            "java/lang/Throwable.getLocalizedMessage()Ljava/lang/String;",
            "java/lang/Throwable.printStackTrace()V",
            "java/lang/Throwable.printStackTrace(Ljava/io/PrintStream;)V",
            "java/lang/Throwable.printStackTrace(Ljava/io/PrintWriter;)V",
            "java/lang/Throwable.getStackTrace()[Ljava/lang/StackTraceElement;",
            "java/lang/Throwable.initCause(Ljava/lang/Throwable;)Ljava/lang/Throwable;",
            "java/lang/Throwable.getSuppressed()[Ljava/lang/Throwable;",
            "java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V",
            "java/util/Collection.spliterator()Ljava/util/Spliterator;",
            // It's ok because we've got a Kotlin defined signature in built-in class declaration
            "java/util/Map.getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            "java/lang/Throwable.<init>(Ljava/lang/String;Ljava/lang/Throwable;ZZ)V"
        )
    }
}
