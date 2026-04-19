/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ConfigurationDirectives : SimpleDirectivesContainer() {
    val WITH_STDLIB by directive("Add Kotlin stdlib to classpath")

    val WITH_KOTLIN_JVM_ANNOTATIONS by directive("Add kotlin-annotations-jvm.jar to classpath")

    val DISABLE_TYPEALIAS_EXPANSION by directive("Disables automatic expansion of aliased types in type resolution")

    val SEPARATE_KMP_COMPILATION by directive("Enables separate compilation for KMP modules")

    val METADATA_ONLY_COMPILATION by directive("Treats all compilations as metadata compilation")

    val WORKS_WHEN_VALUE_CLASS by directive(
        "Enables `JvmInlineSourceTransformer`, which transforms the OPTIONAL_JVM_INLINE_ANNOTATION placeholder"
    )

    val TARGET_BACKEND by enumDirective<TargetBackend>("If specified then test will be skipped on any non-specified backends")
    val DONT_TARGET_EXACT_BACKEND by enumDirective<TargetBackend>("If specified then test will be skipped on specified backends")

    val METADATA_TARGET_PLATFORMS by stringDirective("List of platforms for which metadata should be generated")

    val KIND by enumDirective<TestKind>(
        description = """
            Usage: // KIND: [REGULAR, STANDALONE, STANDALONE_NO_TR, STANDALONE_LLDB, STANDALONE_STEPPING]
            Declares the kind of the test:

            - REGULAR (the default) - include this test into the shared test binary.
              All tested functions should be annotated with @kotlin.Test.

            - STANDALONE - compile the test to a separate test binary.
              All tested functions should be annotated with @kotlin.Test

            - STANDALONE_NO_TR - compile the test to a separate binary that is supposed to have main entry point.
              The entry point can be customized Note that @kotlin.Test annotations are ignored.

            - STANDALONE_LLDB - compile the test to a separate binary and debug with LLDB, by executing specific LLDB commands.
            
            - STANDALONE_STEPPING - compile the test to a separate binary and debug with LLDB, by stepping through the entire program.
        """.trimIndent()
    )
}

enum class TestKind {
    REGULAR,
    STANDALONE,
    STANDALONE_NO_TR,
    STANDALONE_LLDB,
    STANDALONE_STEPPING;
}

val RegisteredDirectives.testKind: TestKind?
    get() = get(ConfigurationDirectives.KIND).let {
        when (it.size) {
            0 -> null
            1 -> it.single()
            else -> fail("Exactly one test kind expected in ${ConfigurationDirectives.KIND} directive: $it")
        }
    }
