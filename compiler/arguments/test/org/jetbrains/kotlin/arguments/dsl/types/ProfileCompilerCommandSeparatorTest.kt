/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProfileCompilerCommandSeparatorTest {

    @Test
    fun testStringRepresentationEvaluatesToCorrectRuntimeValue() {
        val profileCommand = ProfileCompilerCommand(
            profilerPath = Paths.get("/profiler/lib.so"),
            command = "start,event=cpu",
            outputDir = Paths.get("/snapshots")
        )
        val expectedStringRepresentation =
            "\"${profileCommand.profilerPath.absolutePathString()}${File.pathSeparator}${profileCommand.command}${File.pathSeparator}${profileCommand.outputDir.absolutePathString()}\""

        val representation = ProfileCompilerCommandType.stringRepresentation(profileCommand)
        assertNotNull(representation)
        val actualStringRepresentation = representation.replace($$"${File.pathSeparator}", File.pathSeparator)

        assertEquals(
            expectedStringRepresentation,
            actualStringRepresentation,
            "When evaluated at runtime, the string representation should use the platform's File.pathSeparator"
        )
    }
}
