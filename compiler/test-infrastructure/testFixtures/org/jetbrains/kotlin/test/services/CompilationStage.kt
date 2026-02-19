/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

enum class CompilationStage {
    /**
     * The first compilation stage that for KLIB-based backends includes translating the source code and producing a KLIB,
     * and for the JVM backend — translating the source code and producing a JAR file.
     */
    FIRST,

    /**
     * The second compilation stage that for KLIB-based backends includes running the compiler backend for producing an
     * executable artifact from KLIBs.
     *
     * This is not applicable for the JVM backend.
     */
    SECOND,
}
