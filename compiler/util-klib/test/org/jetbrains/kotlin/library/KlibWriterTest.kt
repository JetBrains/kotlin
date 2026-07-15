/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.KlibWriterTest.NewKlibWriterParameters
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

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
        }.writeTo(createNewKlibDir())

        // Invalid name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir())
        }

        // Invalid name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("   ")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir())
        }

        // No module name
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir())
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
        }.writeTo(createNewKlibDir())

        // No versions
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(createNewKlibDir())
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
            }.writeTo(createNewKlibDir())
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
        }.writeTo(createNewKlibDir())

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.COMMON, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir())
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.JVM)
            }
        }.writeTo(createNewKlibDir())

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.JVM, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir())
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.JS)
            }
        }.writeTo(createNewKlibDir())

        // Unsupported targets
        assertThrows<IllegalStateException> {
            KlibWriter {
                manifest {
                    moduleName("sample")
                    versions(MOCK_VERSIONS)
                    platformAndTargets(BuiltInsPlatform.JS, listOf("foo", "bar"))
                }
            }.writeTo(createNewKlibDir())
        }

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.NATIVE)
            }
        }.writeTo(createNewKlibDir())

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.NATIVE, listOf("foo", "bar"))
            }
        }.writeTo(createNewKlibDir())

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.WASM)
            }
        }.writeTo(createNewKlibDir())

        // OK
        KlibWriter {
            manifest {
                moduleName("sample")
                versions(MOCK_VERSIONS)
                platformAndTargets(BuiltInsPlatform.WASM, listOf("foo", "bar"))
            }
        }.writeTo(createNewKlibDir())
    }

    override fun writeKlib(parameters: NewKlibWriterParameters): Path {
        val klibLocation = createNewKlibDir()

        KlibWriter {
            format(if (parameters.nopack) KlibFormat.Directory else KlibFormat.ZipArchive)

            includeMetadata(parameters.metadata)
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

        }.writeTo(klibLocation)

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
