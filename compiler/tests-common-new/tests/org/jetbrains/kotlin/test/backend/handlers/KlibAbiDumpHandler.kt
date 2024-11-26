/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiReadingFilter.*
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
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
 * Note: It's necessary to activate [KlibAbiDumpDirectives.DUMP_KLIB_ABI] directive and specify one of
 * [KlibAbiDumpDirectives.KlibAbiDumpMode]s to allow this handler dumping ABI.
 */
@OptIn(ExperimentalLibraryAbiReader::class)
class KlibAbiDumpHandler(testServices: TestServices) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers get() = listOf(KlibAbiDumpDirectives)

    private val dumpers = hashMapOf<AbiSignatureVersion, MultiModuleInfoDumper>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val dumpMode = module.directives.singleOrZeroValue(KlibAbiDumpDirectives.DUMP_KLIB_ABI) ?: return

        val libraryAbi = LibraryAbiReader.readAbiInfo(
            info.outputFile,
            ExcludedPackages(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_PACKAGES]),
            ExcludedClasses(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_CLASSES]),
            NonPublicMarkerAnnotations(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_NON_PUBLIC_MARKERS])
        )

        for (abiSignatureVersion in dumpMode.abiSignatureVersions) {
            val dumper = dumpers.getOrPut(abiSignatureVersion) { MultiModuleInfoDumper() }
            LibraryAbiRenderer.render(libraryAbi, dumper.builderForModule(module), AbiRenderingSettings(abiSignatureVersion))
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumpers.isEmpty()) return

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
        val DEFAULT_ABI_SIGNATURE_VERSION: KotlinIrSignatureVersion = KotlinIrSignatureVersion.V2

        fun abiDumpFileExtension(abiSignatureVersion: Int): String {
            val suffix = if (abiSignatureVersion == DEFAULT_ABI_SIGNATURE_VERSION.number) "" else "sig_v$abiSignatureVersion."
            return "${suffix}klib_abi.txt"
        }
    }
}
