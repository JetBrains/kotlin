/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import org.jetbrains.kotlin.konan.target.CompilerOutputKind as CompilerCompilerOutputKind

class CompilerOutputKindConsistency {
    @Test
    fun allKindsAreSupportedByCompiler() {
        // The DSL type intentionally lists only the kinds documented for '-produce', so the compiler may support more of them.
        val compilerKinds = CompilerCompilerOutputKind.entries.map { it.name.lowercase() }
        CompilerOutputKind.entries.filter { it.releaseVersionsMetadata.removedVersion == null }.forEach { kind ->
            assertTrue(
                actual = kind.kindName in compilerKinds,
                message = "Output kind '${kind.kindName}' is not supported by the compiler, while $kind 'removedVersion' is 'null'!"
            )
        }
    }
}
