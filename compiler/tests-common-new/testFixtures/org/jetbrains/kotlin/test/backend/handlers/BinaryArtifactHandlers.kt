/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JvmClassFileArtifact
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class JvmBinaryArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = true
) : BinaryArtifactHandler<BinaryArtifacts.Jvm>(
    testServices,
    ArtifactKinds.Jvm,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures,
) {
    @OptIn(ExperimentalContracts::class)
    protected fun checkArtifact(info: BinaryArtifacts.Jvm) {
        contract {
            returns() implies (info is JvmClassFileArtifact)
        }
        require(info is JvmClassFileArtifact)
    }
}

abstract class JsBinaryArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : BinaryArtifactHandler<BinaryArtifacts.Js>(
    testServices,
    ArtifactKinds.Js,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures
)

abstract class KlibArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures
)

abstract class NativeBinaryArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : BinaryArtifactHandler<BinaryArtifacts.Native>(
    testServices,
    ArtifactKinds.Native,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures
)

abstract class WasmBinaryArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : BinaryArtifactHandler<BinaryArtifacts.Wasm>(
    testServices,
    ArtifactKinds.Wasm,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures
)
