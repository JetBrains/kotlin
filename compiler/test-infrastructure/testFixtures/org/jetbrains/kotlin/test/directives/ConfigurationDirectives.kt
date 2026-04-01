/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
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
}
