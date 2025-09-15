/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.forTestCompile

enum class JavaForeignAnnotationType(val path: String) {
    Annotations(System.getProperty(TestCompilePaths.KOTLIN_THIRDPARTY_ANNOTATIONS_PATH) ?: "third-party/annotations"),
    Java8Annotations(System.getProperty(TestCompilePaths.KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH) ?: "third-party/java8-annotations"),
    Java9Annotations(System.getProperty(TestCompilePaths.KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH) ?: "third-party/java9-annotations"),
    Jsr305(System.getProperty(TestCompilePaths.KOTLIN_THIRDPARTY_JSR305_PATH) ?: "third-party/jsr305")
}
