/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.test.handlers

import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

class LibraryAbiDumpHandler(testServices: TestServices) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    private val dumpers = listOf(
        setOf(AbiSignatureVersion.V1) to MultiModuleInfoDumper(),
        // TODO(KT-59486): setOf(AbiSignatureVersion.V2) to MultiModuleInfoDumper(),
        // TODO(KT-59486): setOf(AbiSignatureVersion.V1, AbiSignatureVersion.V2) to MultiModuleInfoDumper(),
    )

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val libraryAbi = LibraryAbiReader.readAbiInfo(info.outputFile)

        for ((abiSignatureVersions, dumper) in dumpers) {
            val abiDump = libraryAbi.topLevelDeclarations.renderTopLevels(AbiRenderingSettings(abiSignatureVersions))
            dumper.builderForModule(module).append(abiDump)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertions.assertAll(
            dumpers.map { (abiSignatureVersions, dumper) ->
                {
                    val versions = abiSignatureVersions.joinToString(separator = "+") { it.name.lowercase() }
                    val expectedFile = testServices
                        .moduleStructure
                        .originalTestDataFiles
                        .first()
                        .withExtension("$versions.txt")
                    assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
                }
            }
        )
    }
}