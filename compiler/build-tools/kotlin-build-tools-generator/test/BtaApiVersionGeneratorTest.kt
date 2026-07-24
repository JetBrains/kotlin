/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.generator.BtaApiVersionGenerator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("BtaApiVersionGenerator tests")
class BtaApiVersionGeneratorTest {
    private val latestVersion = KotlinReleaseVersion.entries.last()
    private val givenPackage = "org.jetbrains.kotlin.greatbta"

    @Test
    @DisplayName("Should generate an internal object named BuildToolsApiVersion")
    fun generatesInternalObject() {
        val generator = BtaApiVersionGenerator(latestVersion, givenPackage)
        val generatedCode = generator.generate().single().second
        assertTrue(
            generatedCode.contains("internal object BuildToolsApiVersion"),
            "Generated code should contain 'internal object BuildToolsApiVersion'"
        )
    }

    @Test
    @DisplayName("Should respect the target package parameter")
    fun respectsTargetPackage() {
        val generator = BtaApiVersionGenerator(latestVersion, givenPackage)
        val result = generator.generate()
        val generatedCode = result.single().second
        assertTrue(generatedCode.contains(givenPackage), "Generated code should contain '$givenPackage'")
        assertEquals(Path(givenPackage.replace(".", "/"), "BuildToolsApiVersion.kt"), result.single().first)
    }

    @Test
    @DisplayName("Generated code should contain @JvmStatic annotation")
    fun containsJvmStaticAnnotation() {
        val generator = BtaApiVersionGenerator(latestVersion, givenPackage)
        val generatedCode = generator.generate().single().second
        assertTrue(
            generatedCode.contains("@JvmStatic"),
            "Generated code should contain '@JvmStatic' annotation, but was:\n$generatedCode"
        )
    }

    @Test
    @DisplayName("Generated code should contain the correct version string")
    fun embedsCorrectVersionString() {
        val generator = BtaApiVersionGenerator(latestVersion, givenPackage)
        val generatedCode = generator.generate().single().second
        assertTrue(
            generatedCode.contains("\"${latestVersion.releaseName}\""),
            "BuildToolsApiVersion.kt should contain the version string '${latestVersion.releaseName}', but was:\n$generatedCode"
        )
    }
}
