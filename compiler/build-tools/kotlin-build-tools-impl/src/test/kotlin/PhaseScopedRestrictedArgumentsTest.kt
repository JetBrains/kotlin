/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.internal.DefaultKotlinLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("Arguments restricted in a single compiler phase")
class PhaseScopedRestrictedArgumentsTest {
    @OptIn(DelicateBuildToolsApi::class)
    private fun nativeArguments(vararg arguments: String) =
        NativeArgumentsImpl().apply { applyCommandLineArguments(arguments.toList()) }

    @Test
    @DisplayName("Native -produce is rejected by klib compilation")
    fun produceIsRejectedByKlibCompilation() {
        val exception = assertThrows<CompilerArgumentsParseException> {
            nativeArguments("-produce", "program").reportRestrictedViolations(DefaultKotlinLogger, CompilerPhase.KLIB_COMPILATION)
        }
        assertEquals(PRODUCE_MESSAGE, exception.message)
    }

    @Test
    @DisplayName("Native -produce is allowed for linking")
    fun produceIsAllowedForLinking() {
        assertDoesNotThrow {
            nativeArguments("-produce", "program").reportRestrictedViolations(DefaultKotlinLogger, CompilerPhase.BACKEND_COMPILATION)
        }
    }

    @Test
    @DisplayName("Native -produce is rejected when the phase of the operation is unknown")
    fun produceIsRejectedWithoutPhase() {
        val exception = assertThrows<CompilerArgumentsParseException> {
            nativeArguments("-produce", "program").reportRestrictedViolations(DefaultKotlinLogger)
        }
        assertEquals(PRODUCE_MESSAGE, exception.message)
    }

    @Test
    @DisplayName("Native -output is rejected regardless of the phase")
    fun outputIsRejectedInEveryPhase() {
        for (phase in CompilerPhase.entries) {
            assertThrows<CompilerArgumentsParseException>("Phase: $phase") {
                nativeArguments("-output", "out").reportRestrictedViolations(DefaultKotlinLogger, phase)
            }
        }
    }

    private companion object {
        const val PRODUCE_MESSAGE =
            "Argument '-produce'/'-p' is not supported in the Build Tools API. Klib compilation always produces a library."
    }
}
