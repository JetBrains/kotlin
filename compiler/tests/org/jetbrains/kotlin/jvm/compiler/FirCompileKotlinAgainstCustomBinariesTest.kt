/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.config.LanguageVersion

class FirCompileKotlinAgainstCustomBinariesTest : AbstractCompileKotlinAgainstCustomBinariesTest() {
    override val languageVersion: LanguageVersion
        get() = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.KOTLIN_2_0)

    override fun muteForK2(test: () -> Unit) {
        try {
            test()
        } catch (e: Throwable) {
            return
        }
        fail("Looks like this test can be unmuted. Remove the call to `muteForK2`.")
    }

    fun testHasStableParameterNames() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    fun testDeserializedAnnotationReferencesJava() {
        // Only Java
        val libraryAnnotation = compileLibrary("libraryAnnotation")
        // Specifically, use K1
        // Remove "-Xuse-k2=false" argument once it becomes forbidden
        val libraryUsingAnnotation = compileLibrary(
            "libraryUsingAnnotation",
            additionalOptions = listOf("-language-version", "1.8", "-Xuse-k2=false"),
            extraClassPath = listOf(libraryAnnotation)
        )

        compileKotlin(
            "usage.kt",
            output = tmpdir,
            classpath = listOf(libraryAnnotation, libraryUsingAnnotation),
        )
    }

    fun testFirAgainstFirUsingFlag() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")), additionalOptions = listOf("-Xuse-k2"))
    }
}
