/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.KlibWriterTest.NewKlibWriterParameters
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.javaPath
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.KlibWrittenMetadataPackageFragmentTracker
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * This is the test for the redesigned (new) KLIB writer API (as the opposite of the test for the legacy one: [LegacyKlibWriterTest]).
 */
class KlibWriterTest : AbstractKlibWriterTest<NewKlibWriterParameters>(::NewKlibWriterParameters) {
    class NewKlibWriterParameters : Parameters() {
        var targetNames: List<String> = emptyList()
    }

    @Test
    fun `Module name validation`() {
        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
        }.writeTo(createNewKlibDir().path)

        // Invalid name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir().path)
        }

        // Invalid name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("   ")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir().path)
        }

        // No module name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir().path)
        }
    }

    @Test
    fun `Versions validation`() {
        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
        }.writeTo(createNewKlibDir().path)

        // No versions
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir().path)
        }
    }

    @Test
    fun `Platform and target validation`() {
        // No platform and targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                }
            }.writeTo(createNewKlibDir().path)
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
        }.writeTo(createNewKlibDir().path)

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir().path)
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.JVM)
            }
        }.writeTo(createNewKlibDir().path)

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.JVM, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir().path)
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.JS)
            }
        }.writeTo(createNewKlibDir().path)

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.JS, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir().path)
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.NATIVE)
            }
        }.writeTo(createNewKlibDir().path)

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.NATIVE, listOf("foo", "bar"))
            }
        }.writeTo(createNewKlibDir().path)

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.WASM)
            }
        }.writeTo(createNewKlibDir().path)

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.WASM, listOf("foo", "bar"))
            }
        }.writeTo(createNewKlibDir().path)
    }

    @Test
    fun `Fragments source file reports`() {
        val recordedMappings = mutableListOf<Pair<Path?, Path>>()
        val tracker = KlibWrittenMetadataPackageFragmentTracker { sourceFile, outputFile -> recordedMappings += sourceFile to outputFile }

        val content = ByteArray(10)
        val klibDir = writeKlib(
            NewKlibWriterParameters().apply {
                fragmentTracker = tracker
                metadata = SerializedMetadata(
                    module = content,
                    fragments = listOf(
                        listOf(
                            SerializedFragmentWithSource(content, "/src/a.kt"),
                            SerializedFragment(content),
                        ),
                        listOf(
                            SerializedFragmentWithSource(content, null),
                        ),
                    ),
                    fragmentNames = listOf("", "foo.bar"),
                    metadataVersion = MetadataVersion.INSTANCE.toArray(),
                )
            }
        )

        val layout = KlibMetadataComponentLayout(KlibFile(klibDir.path))
        val expectedMappings = listOf(
            null to layout.getPackageFragmentFile(packageFqName = "", partName = "0_").javaPath(),
            Path("/src/a.kt") to layout.getPackageFragmentFile(packageFqName = "", partName = "a").javaPath(),
            null to layout.getPackageFragmentFile(packageFqName = "foo.bar", partName = "0_bar").javaPath(),
        )

        assertEquals(
            expectedMappings.map { (source, output) -> source to output },
            recordedMappings.map { (source, output) -> source to output },
        )
    }

    @Test
    fun `Fragments source file with the same name and package reports`() {
        val content = ByteArray(10)

        assertThrows<IllegalStateException> {
            writeKlib(
                NewKlibWriterParameters().apply {
                    metadata = SerializedMetadata(
                        module = content,
                        fragments = listOf(
                            listOf(
                                SerializedFragmentWithSource(content, "/src/x/a.kt"),
                                SerializedFragmentWithSource(content, "/src/y/a.kt"),
                            ),
                        ),
                        fragmentNames = listOf("foo.bar"),
                        metadataVersion = MetadataVersion.INSTANCE.toArray(),
                    )
                }
            )
        }
    }

    @Test
    fun `Existing files removal control`() {
        fun newWriter(allowIncrementalOverwriting: Boolean) = KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
            allowIncrementalOverwriting(allowIncrementalOverwriting)
        }

        val klibDir = createNewKlibDir()
        val staleFile = File(klibDir, "stale.txt").apply { writeText("stale") }

        newWriter(true).writeTo(klibDir.path)
        assertTrue(staleFile.exists(), "Pre-existing file must be preserved when allowIncrementalOverwriting = true")

        newWriter(false).writeTo(klibDir.path)
        assertFalse(staleFile.exists(), "Pre-existing file must be removed when allowIncrementalOverwriting = false")
    }

    override fun writeKlib(parameters: NewKlibWriterParameters): File {
        val klibLocation = createNewKlibDir()

        KlibWriter {
            format(if (parameters.nopack) KlibFormat.Directory else KlibFormat.ZipArchive)

            includeMetadata(parameters.metadata, parameters.fragmentTracker)
            includeIr(parameters.ir)

            manifest {
                moduleName(parameters.uniqueName)
                platformAndTargets(parameters.builtInsPlatform, parameters.targetNames)
                versions(
                    KotlinLibraryVersioning(
                        compilerVersion = parameters.compilerVersion,
                        metadataVersion = parameters.metadataVersion,
                        abiVersion = parameters.abiVersion,
                    )
                )
                customProperties { this += parameters.customManifestProperties }
            }

        }.writeTo(klibLocation.path)

        return klibLocation
    }

    companion object {
        private val MOCK_VERSIONS = KotlinLibraryVersioning(
            compilerVersion = null,
            abiVersion = null,
            metadataVersion = null,
        )
    }
}
