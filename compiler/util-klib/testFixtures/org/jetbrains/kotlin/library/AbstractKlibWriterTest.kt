/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.AbstractKlibWriterTest.Parameters
import org.jetbrains.kotlin.library.AbstractKlibWriterTest.Parameters.KlibDependency
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.util.UUID
import org.jetbrains.kotlin.konan.file.File as KlibFile

abstract class AbstractKlibWriterTest<P : Parameters>(private val newParameters: () -> P) {
    open class Parameters {
        var uniqueName: String = "foo"
        var builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.COMMON
        var compilerVersion: String? = null
        var metadataVersion: MetadataVersion? = null
        var abiVersion: KotlinAbiVersion? = null
        var customManifestProperties: List<Pair<String, String>> = emptyList()
        var nopack: Boolean = true
        var dependencies: List<KlibDependency> = emptyList()
        var ir: Collection<SerializedIrFile>? = null
        var irOfInlinableFunctions: SerializedIrFile? = null

        // Note: There is always some randomly generated metadata. Because there is no way to generate a klib without metadata.
        val metadata: SerializedMetadata = KlibMockDSL.generateRandomMetadata()

        class KlibDependency(val uniqueName: String, val path: String)
    }

    private lateinit var tmpDir: File

    @BeforeEach
    fun setup(info: TestInfo) {
        tmpDir = createTempDirectory(info.testClass.get().simpleName + "-" + info.testMethod.get().name).toRealPath().toFile()
    }

    @AfterEach
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `Writing a klib with different unique names`() {
        listOf("foo", "bar-bar", "something.Different.X64").forEach { uniqueName ->
            runTestWithParameters {
                this.uniqueName = uniqueName
            }
        }
    }

    @Test
    fun `Writing a klib with different compiler versions`() {
        listOf(null, "1.2.3", "1.2.3-rc-456", "1.2.3-SNAPSHOT").forEach { compilerVersion ->
            runTestWithParameters {
                this.compilerVersion = compilerVersion
            }
        }
    }

    @Test
    fun `Writing a klib with different metadata versions`() {
        listOf(null, MetadataVersion.INSTANCE, MetadataVersion.INSTANCE_NEXT, MetadataVersion(100, 500)).forEach { metadataVersion ->
            runTestWithParameters {
                this.metadataVersion = metadataVersion
            }
        }
    }

    @Test
    fun `Writing a klib with different ABI versions`() {
        listOf(null, KotlinAbiVersion.CURRENT, KotlinAbiVersion(100, 500, 1000)).forEach { abiVersion ->
            runTestWithParameters {
                this.abiVersion = abiVersion
            }
        }
    }

    @Test
    fun `Writing a klib with custom manifest properties`() {
        runTestWithParameters {
            customManifestProperties = listOf("key1" to "value1", "key2" to "value2 value3")
        }
    }

    @Test
    fun `Writing a klib packed and unpacked`() {
        listOf(true, false).forEach { nopack ->
            runTestWithParameters {
                this.nopack = nopack
            }
        }
    }

    @Test
    fun `Writing a klib with IR`() {
        for (mainIrFiles in 0..5) {
            listOf(true, false).forEach { includeInlinableFunctions ->
                runTestWithParameters {
                    ir = List(mainIrFiles) { KlibMockDSL.generateRandomIrFile() }
                    irOfInlinableFunctions = if (includeInlinableFunctions) KlibMockDSL.generateRandomIrFile() else null
                }
            }
        }
    }

    @Test
    fun `Writing a klib with dependencies`() {
        runTestWithParameters {
            dependencies = listOf(mockKlibDependency("dep1"), mockKlibDependency("dep2"))
        }
    }

    protected fun runTestWithParameters(initializeParameters: P.() -> Unit) {
        val parameters = newParameters()
        parameters.initializeParameters()

        val writtenKlib = writeKlib(parameters).unpackIfNecessary(parameters)

        val mockKlib = KlibMockDSL.mockKlib(createNewKlibDir()) {
            metadata(parameters.metadata)
            parameters.ir?.let { ir(it) }
            parameters.irOfInlinableFunctions?.let { irInlinableFunctions(it) }
            resources()
            manifest(
                uniqueName = parameters.uniqueName,
                builtInsPlatform = parameters.builtInsPlatform,
                versioning = KotlinLibraryVersioning(
                    compilerVersion = parameters.compilerVersion,
                    abiVersion = parameters.abiVersion,
                    metadataVersion = parameters.metadataVersion,
                ),
                other = {
                    if (parameters.dependencies.isNotEmpty()) {
                        this[KLIB_PROPERTY_DEPENDS] = parameters.dependencies.joinToString(" ") { it.uniqueName }
                    }
                    parameters.customManifestProperties.forEach { (key, value) -> this[key] = value }
                    customizeManifestForMockKlib(parameters)
                }
            )
        }

        val result = KlibFileSystemDiff(mockKlib, writtenKlib).recursiveDiff()
        if (result !is KlibFileSystemDiff.Result.Identical) {
            fail(result.toString())
        }
    }

    protected abstract fun writeKlib(parameters: P): File

    private fun File.unpackIfNecessary(parameters: P): File {
        if (parameters.nopack) return this

        return createNewKlibDir().also { unzippedDir -> this.unzipTo(unzippedDir) }
    }

    protected open fun Properties.customizeManifestForMockKlib(parameters: P) = Unit

    protected fun createNewKlibDir(): File = tmpDir.resolve(UUID.randomUUID().toString()).apply(File::mkdirs)

    protected fun mockKlibDependency(uniqueName: String): KlibDependency = KlibDependency(
        uniqueName = uniqueName,
        path = KlibMockDSL.mockKlib(createNewKlibDir()) {
            manifest(
                uniqueName = uniqueName,
                builtInsPlatform = BuiltInsPlatform.COMMON,
                versioning = KotlinLibraryVersioning(null, null, null)
            )
        }.path
    )

    companion object {
        private fun File.toKlibFile() = KlibFile(absolutePath)

        private fun File.unzipTo(destinationDirectory: File) {
            toKlibFile().unzipTo(destinationDirectory.toKlibFile())
        }
    }
}
