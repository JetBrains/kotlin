/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices

abstract class JvmBinaryArtifactHandler(
    testServices: TestServices,
) : BinaryArtifactHandler<BinaryArtifacts.Jvm>(
    testServices,
    ArtifactKinds.Jvm
) {
    override val doNotRunIfThereWerePreviousFailures: Boolean = true
}

abstract class JsBinaryArtifactHandler(
    testServices: TestServices
) : BinaryArtifactHandler<BinaryArtifacts.Js>(
    testServices,
    ArtifactKinds.Js
)

abstract class KlibArtifactHandler(
    testServices: TestServices
) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib
)

abstract class NativeBinaryArtifactHandler(
    testServices: TestServices,
) : BinaryArtifactHandler<BinaryArtifacts.Native>(
    testServices,
    ArtifactKinds.Native
)

abstract class WasmBinaryArtifactHandler(
    testServices: TestServices,
) : BinaryArtifactHandler<BinaryArtifacts.Wasm>(
    testServices,
    ArtifactKinds.Wasm
)
