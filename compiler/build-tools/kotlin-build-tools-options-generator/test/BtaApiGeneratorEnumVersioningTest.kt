/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.types.AbiStabilityModeType
import org.jetbrains.kotlin.arguments.dsl.types.AssertionsModeType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.buildtools.options.generator.API_ARGUMENTS_PACKAGE
import org.jetbrains.kotlin.buildtools.options.generator.API_ENUMS_PACKAGE
import org.jetbrains.kotlin.buildtools.options.generator.BtaApiGenerator
import org.jetbrains.kotlin.buildtools.options.generator.SinceVersionsRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BtaApiGeneratorEnumVersioningTest {

    @TempDir
    private lateinit var genDir: Path

    @TempDir
    private lateinit var registryDir: Path

    private lateinit var testJvmCompilerArgumentsLevel: KotlinCompilerArgumentsLevel
    private lateinit var testJvmCompilerArguments: Set<KotlinCompilerArgument>

    @BeforeEach
    fun setup() {
        testJvmCompilerArguments = setOf(createKotlinCompilerArgument("assertions", AssertionsModeType()))
        testJvmCompilerArgumentsLevel = createKotlinCompilerArgumentsLevel(testJvmCompilerArguments)
    }

    @Test
    @DisplayName("Newly generated enum classes receive the selected kotlinVersion as their @since")
    fun newEnumFilesGetCurrentVersion() {
        val kotlinVersion = KotlinReleaseVersion.v2_0_0

        val files = generateAndWriteArgumentsForLevel(kotlinVersion).enumFiles()
        assertTrue(files.isNotEmpty(), "Expected at least one enum file to be generated")

        files.forEach { (path, content) ->
            assertTrue(
                "@since ${kotlinVersion.releaseName}" in content,
                "New enum file $path should carry @since ${kotlinVersion.releaseName}"
            )
        }
    }

    @Test
    @DisplayName("Re-running the generator assigns the current version to new enums and preserves @since for existing ones")
    fun existingEnumFilePreservesItsVersion() {
        val firstKotlinVersion = KotlinReleaseVersion.v2_0_0
        val firstOutputEnums = generateAndWriteArgumentsForLevel(firstKotlinVersion).enumFiles().toMap()
        val secondKotlinVersion = KotlinReleaseVersion.v2_4_0
        val expandedLevel = createKotlinCompilerArgumentsLevel(
            testJvmCompilerArguments + createKotlinCompilerArgument("abiStabilityMode", AbiStabilityModeType())
        )

        val secondOutputEnums = generateAndWriteArgumentsForLevel(secondKotlinVersion, expandedLevel).enumFiles().toMap()
        val existingEnumPaths = secondOutputEnums.keys.intersect(firstOutputEnums.keys)
        val newEnumPaths = secondOutputEnums.keys - firstOutputEnums.keys

        assertTrue(newEnumPaths.isNotEmpty(), "Expected at least one new enum file to be generated")
        newEnumPaths.forEach { path ->
            val content = secondOutputEnums[path] ?: error("Expected new enum file $path to be generated")
            assertTrue(
                "@since ${secondKotlinVersion.releaseName}" in content,
                "New enum file $path should carry @since ${secondKotlinVersion.releaseName}"
            )
        }
        assertTrue(existingEnumPaths.isNotEmpty(), "Expected at least one existing enum file to be preserved")
        existingEnumPaths.forEach { path ->
            val content = secondOutputEnums[path] ?: error("Expected existing enum file $path to be generated")
            assertTrue(
                "@since ${firstKotlinVersion.releaseName}" in content,
                "Existing enum file $path should carry @since ${firstKotlinVersion.releaseName}"
            )
        }
    }

    @Test
    @DisplayName("Re-running the generator with same version preserves the version from the registry")
    fun registryVersionPreservedOnRegeneration() {
        val kotlinVersion = KotlinReleaseVersion.v2_0_0
        generateAndWriteArgumentsForLevel(kotlinVersion)

        val (_, content) = generateAndWriteArgumentsForLevel(kotlinVersion).enumFiles().first()

        assertTrue(
            "@since ${kotlinVersion.releaseName}" in content,
            "After regeneration, version should be re-read from the registry unchanged"
        )
    }

    @Test
    @DisplayName("Removed enum class is absent from generated output")
    fun staleEnumFileIsAbsentFromGeneratedOutput() {
        val enumsDir = genDir.resolve("org/jetbrains/kotlin/buildtools/api/arguments/enums").apply {
            toFile().mkdirs()
        }
        val stalePath = enumsDir.resolve("ObsoleteMode.kt").apply {
            writeText(
                "/** @since 2.3.0 */\nenum class ObsoleteMode { ENTRY }"
            )
        }

        val generatedPaths = generateAndWriteArgumentsForLevel(KotlinReleaseVersion.v2_0_0).map { it.first }
        val relativeStale = genDir.relativize(stalePath)

        assertTrue(
            generatedPaths.any { it.toString().contains(API_ENUMS_PACKAGE.replace('.', '/')) },
            "Expected at least one enum file to be generated"
        )
        assertFalse(
            relativeStale in generatedPaths,
            "Stale enum file not referenced by any argument should not appear in generator outputs"
        )
    }

    private fun createKotlinCompilerArgument(name: String, valueType: KotlinArgumentValueType<*>) =
        KotlinCompilerArgument(
            name = name,
            description = ReleaseDependent(
                current = "Fake argument for testing.",
                valueInVersions = emptyMap()
            ),
            valueType = valueType,
            releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
                introducedVersion = KotlinReleaseVersion.v1_0_0,
            ),
            delimiter = null,
        )

    private fun createKotlinCompilerArgumentsLevel(arguments: Set<KotlinCompilerArgument>) =
        KotlinCompilerArgumentsLevel(
            name = "jvmCompilerArguments",
            arguments = arguments,
            nestedLevels = emptySet(),
        )

    private fun generateAndWriteArgumentsForLevel(
        kotlinVersion: KotlinReleaseVersion,
        level: KotlinCompilerArgumentsLevel = testJvmCompilerArgumentsLevel,
    ): List<Pair<Path, String>> {
        val registry = SinceVersionsRegistry(registryDir.resolve("since-versions.properties"))
        val generator = createBtaApiGenerator(kotlinVersion, registry)
        val output = generator.generateArgumentsForLevel(level)
        output.generatedFiles.forEach { (relativePath, content) ->
            genDir.resolve(relativePath).toFile().also { it.parentFile.mkdirs() }.writeText(content)
        }
        registry.writeToFile()
        return output.generatedFiles
    }

    private fun createBtaApiGenerator(kotlinVersion: KotlinReleaseVersion, registry: SinceVersionsRegistry) =
        BtaApiGenerator(API_ARGUMENTS_PACKAGE, skipXX = true, kotlinVersion, registry)

    private fun List<Pair<Path, String>>.enumFiles() =
        filter { (path, _) -> path.toString().contains(API_ENUMS_PACKAGE.replace('.', '/')) }
}
