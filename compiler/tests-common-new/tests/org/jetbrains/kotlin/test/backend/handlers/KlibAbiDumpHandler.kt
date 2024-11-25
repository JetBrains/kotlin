/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiReadingFilter.*
import org.jetbrains.kotlin.library.abi.impl.AbiSignatureVersions
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

/**
 * Dumps KLIB ABI in the format of [LibraryAbiReader].
 *
 * Instead of calling [KlibAbiDumpHandler]'s constructor, which is private, please use either of these functions:
 * - [withAllSupportedSignatureVersions] to generate dump files for all currently supported versions of signatures.
 *   See [KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS] for the list of such.
 * - [withOnlyDefaultSignatureVersion] to generate dump files only for the current "default" signature version,
 *   which corresponds to [DEFAULT_ABI_SIGNATURE_VERSION].
 *
 * @property dumpedSignatureVersions The versions of IR signatures to be rendered in each dump file (a dump file
 * per a signature version).
 */
@OptIn(ExperimentalLibraryAbiReader::class)
class KlibAbiDumpHandler private constructor(
    testServices: TestServices,
    dumpedSignatureVersions: Set<KotlinIrSignatureVersion>,
) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers get() = listOf(KlibAbiDumpDirectives)

    init {
        check(dumpedSignatureVersions.isNotEmpty()) { "At least one signature version must be specified" }
    }

    private val dumpers = dumpedSignatureVersions.map { irSignatureVersion ->
        AbiSignatureVersions.resolveByVersionNumber(irSignatureVersion.number) to MultiModuleInfoDumper()
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val libraryAbi = LibraryAbiReader.readAbiInfo(
            info.outputFile,
            ExcludedPackages(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_PACKAGES]),
            ExcludedClasses(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_CLASSES]),
            NonPublicMarkerAnnotations(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_NON_PUBLIC_MARKERS])
        )

        for ((abiSignatureVersion, dumper) in dumpers) {
            LibraryAbiRenderer.render(libraryAbi, dumper.builderForModule(module), AbiRenderingSettings(abiSignatureVersion))
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertions.assertAll(
            dumpers.map { (abiSignatureVersion, dumper) ->
                val dumpFileExtension = abiDumpFileExtension(abiSignatureVersion.versionNumber)
                val lambda = {
                    val expectedFile = testServices
                        .moduleStructure
                        .originalTestDataFiles
                        .first()
                        .withExtension(dumpFileExtension)
                    assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
                }
                lambda
            }
        )
    }

    companion object {
        private val DEFAULT_ABI_SIGNATURE_VERSION: KotlinIrSignatureVersion = KotlinIrSignatureVersion.V2

        /** Create a [KlibAbiDumpHandler] that would generate dump files for all currently supported versions of signatures. */
        fun withAllSupportedSignatureVersions(testServices: TestServices): KlibAbiDumpHandler =
            KlibAbiDumpHandler(testServices, dumpedSignatureVersions = KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS)

        /** Create a [KlibAbiDumpHandler] that would generate dump files only for the current "default" versions of signatures. */
        fun withOnlyDefaultSignatureVersion(testServices: TestServices): KlibAbiDumpHandler =
            KlibAbiDumpHandler(testServices, dumpedSignatureVersions = setOf(DEFAULT_ABI_SIGNATURE_VERSION))

        fun abiDumpFileExtension(abiSignatureVersion: Int): String {
            val suffix = if (abiSignatureVersion == DEFAULT_ABI_SIGNATURE_VERSION.number) "" else "sig_v$abiSignatureVersion."
            return "${suffix}klib_abi.txt"
        }
    }
}
