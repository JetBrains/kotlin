/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.buildtools.options.generator.SinceVersionsRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

internal class DetermineSinceVersionTest {

    @TempDir
    private lateinit var tempDir: Path

    private val currentVersion = "2.4.0"
    private val className = ClassName("org.jetbrains.kotlin.buildtools.api.arguments.enums", "SomeMode")

    private val registryFile get() = tempDir.resolve("since-versions.properties")

    @Test
    @DisplayName("Returns currentVersion when the registry file does not exist")
    fun missingRegistryFileUsesCurrentVersion() {
        val result = SinceVersionsRegistry(registryFile).getOrPut(className, currentVersion)

        assertEquals(currentVersion, result)
    }

    @Test
    @DisplayName("Returns the version stored in the registry for an existing entry")
    fun existingEntryPreservesItsVersion() {
        registryFile.writeText("${className.canonicalName}=2.3.0\n")

        val result = SinceVersionsRegistry(registryFile).getOrPut(className, currentVersion)

        assertEquals("2.3.0", result)
    }

    @Test
    @DisplayName("Returns currentVersion and adds the entry when the class is not in the registry")
    fun newClassGetsCurrentVersionAndIsPersisted() {
        registryFile.writeText("org.example.OtherClass=2.3.0\n")
        val registry = SinceVersionsRegistry(registryFile)

        val result = registry.getOrPut(className, currentVersion)
        registry.writeToFile()

        assertEquals(currentVersion, result)
        val written = registryFile.readText()
        assert("${className.canonicalName}=$currentVersion" in written) {
            "Expected new entry in registry, got:\n$written"
        }
    }

    @Test
    @DisplayName("writeToFile produces alphabetically sorted key=value lines")
    fun writeToFileProducesSortedOutput() {
        val registry = SinceVersionsRegistry(registryFile)
        registry.getOrPut(ClassName("org.example", "ZClass"), "2.4.0")
        registry.getOrPut(ClassName("org.example", "AClass"), "2.3.0")
        registry.writeToFile()

        val lines = registryFile.readText().trimEnd().lines()
        assertEquals("org.example.AClass=2.3.0", lines[0])
        assertEquals("org.example.ZClass=2.4.0", lines[1])
    }
}
