/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AppleToolInvocationTest {

    @Test
    fun `commandLine puts the executable first`() {
        val invocation = AppleToolInvocation(path("/usr/bin/lipo"), listOf("-create", "a b.a"))
        assertEquals(listOf("/usr/bin/lipo", "-create", "a b.a"), invocation.commandLine())
    }

    @Test
    fun `toString quotes only arguments that need it`() {
        val invocation = AppleToolInvocation(
            path("/usr/bin/xcrun"),
            listOf("lipo", "-output", "/tmp/out dir/lib.a", "it's", "", "KEY=value", "ünï"),
        )
        assertEquals(
            """/usr/bin/xcrun lipo -output '/tmp/out dir/lib.a' 'it'\''s' '' KEY=value 'ünï'""",
            invocation.toString(),
        )
    }

    @Test
    fun `toString puts the variables it sets in front, sorted`() {
        val invocation = AppleToolInvocation(
            path("/usr/bin/xcrun"),
            listOf("lipo"),
            environment = mapOf("TOOLCHAINS" to "swift", "DEVELOPER_DIR" to "/Applications/Xcode 16.app/Contents/Developer"),
        )
        assertEquals(
            "DEVELOPER_DIR='/Applications/Xcode 16.app/Contents/Developer' TOOLCHAINS=swift /usr/bin/xcrun lipo",
            invocation.toString(),
        )
    }

    @Test
    fun `applyEnvironment removes names and prefixes before setting variables`() {
        val invocation = AppleToolInvocation(
            path("/usr/bin/xcrun"),
            emptyList(),
            environment = mapOf("DEVELOPER_DIR" to "/X.app/Contents/Developer", "SDKROOT" to "kept because it is set"),
            environmentCleanup = EnvironmentCleanup(names = setOf("SDKROOT", "ENABLE_DEBUG_DYLIB"), prefixes = setOf("OTHER_")),
        )
        val environment = mutableMapOf(
            "PATH" to "/usr/bin",
            "SDKROOT" to "/sdk",
            "ENABLE_DEBUG_DYLIB" to "YES",
            "OTHER_LDFLAGS" to "-lfoo",
            "MY_OTHER_VAR" to "kept",
            "DEVELOPER_DIR" to "/old",
        )

        invocation.applyEnvironment(environment)

        assertEquals(
            mapOf(
                "PATH" to "/usr/bin",
                "MY_OTHER_VAR" to "kept",
                "DEVELOPER_DIR" to "/X.app/Contents/Developer",
                "SDKROOT" to "kept because it is set",
            ),
            environment,
        )
    }

    @Test
    fun `a relative executable is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> { AppleToolInvocation(path("lipo"), emptyList()) }
        assertEquals("The executable must be an absolute path, got 'lipo'", exception.message)
    }

    @Test
    fun `no accepted exit codes is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AppleToolInvocation(path("/usr/bin/true"), emptyList(), acceptedExitCodes = emptySet())
        }
    }

    @Test
    fun `applyEnvironment works on a map of any values`() {
        val invocation = AppleToolInvocation(
            path("/usr/bin/xcrun"), emptyList(), environment = mapOf("A" to "1"),
            environmentCleanup = EnvironmentCleanup(names = setOf("B")),
        )
        val environment = mutableMapOf<String, Any>("B" to 2, "C" to listOf(3))

        invocation.applyEnvironment(environment)

        assertEquals(mapOf<String, Any>("C" to listOf(3), "A" to "1"), environment)
    }

    @Test
    fun `copy keeps everything it is not given`() {
        val invocation = AppleToolInvocation(
            path("/usr/bin/xcrun"), listOf("simctl"), workingDirectory = path("/work"), environment = mapOf("A" to "1"),
            environmentCleanup = EnvironmentCleanup.SDKROOT, input = "text",
        )

        val changed = invocation.copy(acceptedExitCodes = setOf(0, 1))

        assertEquals(setOf(0, 1), changed.acceptedExitCodes)
        assertEquals(invocation.toString(), changed.toString())
        assertEquals(invocation.workingDirectory, changed.workingDirectory)
        assertEquals(invocation.environmentCleanup, changed.environmentCleanup)
        assertEquals("text", changed.input)
    }

    @Test
    fun `Forward sends stderr to the same consumer unless told otherwise`() {
        val consumer: (String) -> Unit = { }
        val errorConsumer: (String) -> Unit = { }

        assertSame(consumer, OutputMode.Forward(consumer).errorConsumer)
        assertSame(errorConsumer, OutputMode.Forward(consumer, errorConsumer).errorConsumer)
    }
}
