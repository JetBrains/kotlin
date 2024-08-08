/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.toMetadataVersion
import java.util.jar.JarFile

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
        val libraryUsingAnnotation = compileLibrary(
            "libraryUsingAnnotation",
            additionalOptions = listOf("-language-version", "1.8"),
            extraClassPath = listOf(libraryAnnotation)
        )

        compileKotlin(
            "usage.kt",
            output = tmpdir,
            classpath = listOf(libraryAnnotation, libraryUsingAnnotation),
        )
    }

    fun testStrictMetadataVersionSemanticsOldVersion() {
        val nextMetadataVersion = languageVersion.toMetadataVersion().next()
        val library = compileLibrary(
            "library", additionalOptions = listOf("-Xgenerate-strict-metadata-version", "-Xmetadata-version=$nextMetadataVersion")
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testPreReleaseFlagIsConsistentBetweenBootstrapAndCurrentCompiler() {
        val bootstrapCompiler = JarFile(PathUtil.kotlinPathsForCompiler.compilerPath)
        val classFromBootstrapCompiler = bootstrapCompiler.getEntry(LanguageFeature::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            bootstrapCompiler.getInputStream(classFromBootstrapCompiler).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }

    fun testPreReleaseFlagIsConsistentBetweenStdlibAndCurrentCompiler() {
        val stdlib = JarFile(PathUtil.kotlinPathsForCompiler.stdlibPath)
        val classFromStdlib = stdlib.getEntry(KotlinVersion::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            stdlib.getInputStream(classFromStdlib).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }
}
