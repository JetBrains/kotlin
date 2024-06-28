/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.abi.impl.AbiSignatureVersions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

/**
 * This test checks that all [KotlinIrSignatureVersion]s known to the current version of the compiler are supported
 * by the same version of the ABI reader. The goal of the test is to make sure that a newly added [KotlinIrSignatureVersion]
 * is not forgotten to be properly supported in the ABI reader.
 *
 * Note, that theoretically it is possible that the library itself is produced by a newer compiler, and thus it might
 * have a newer [KotlinIrSignatureVersion] that is not yet supported by the current version of the ABI reader.
 * This is a legal situation. And it is not covered by this test.
 */
@OptIn(ExperimentalLibraryAbiReader::class)
class AllKnownIrSignatureVersionsAreSupported {
    @Test
    fun test() {
        KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.forEach { irSignatureVersion ->
            val abiSignatureVersion = AbiSignatureVersions.resolveByVersionNumber(irSignatureVersion.number)
            assertTrue(abiSignatureVersion.isSupportedByAbiReader) {
                "IR signature version $irSignatureVersion is not supported by the ABI reader"
            }
        }
    }
}
