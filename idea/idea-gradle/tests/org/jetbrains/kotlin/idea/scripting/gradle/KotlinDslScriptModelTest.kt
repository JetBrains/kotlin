/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.gradle.groovy.scripts.TextResourceScriptSource
import org.gradle.internal.exceptions.LocationAwareException
import org.gradle.internal.resource.UriTextResource
import org.jetbrains.kotlin.idea.scripting.gradle.importing.parsePositionFromException
import org.junit.Test
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KotlinDslScriptModelTest {
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testExceptionPositionParsing() {
        val file = createTempDirectory("kotlinDslTest") / "build.gradle.kts"
        val line = 10

        val mockScriptSource = TextResourceScriptSource(UriTextResource("build file", file.toFile()))
        val mockException = LocationAwareException(RuntimeException(), mockScriptSource, line)
        val fromException = parsePositionFromException(mockException.stackTraceToString())
        assertNotNull(fromException, "Position should be parsed")
        assertEquals(fromException.first, file.toAbsolutePath().toString(), "Wrong file name parsed")
        assertEquals(fromException.second.line, line, "Wrong line number parsed")

        file.parent.deleteExisting()
    }
}