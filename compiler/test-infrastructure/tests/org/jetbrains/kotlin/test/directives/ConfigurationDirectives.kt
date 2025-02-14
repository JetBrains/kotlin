/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ConfigurationDirectives : SimpleDirectivesContainer() {
    val WITH_STDLIB by directive("Add Kotlin stdlib to classpath")

    val WITH_KOTLIN_JVM_ANNOTATIONS by directive("Add kotlin-annotations-jvm.jar to classpath")

    val DISABLE_TYPEALIAS_EXPANSION by directive("Disables automatic expansion of aliased types in type resolution")

    val SKIP_K1 by directive("Skip K1 tests")
}
