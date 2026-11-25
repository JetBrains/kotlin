/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlistBuddyTest {
    private var response = result()
    private val runner = RecordingAppleToolRunner { response }
    private val plistBuddy = AppleTools(fakeXcode, runner).plistBuddy

    @Test
    fun `commands are passed with -c before the plist`() {
        val invocation = plistBuddy.invocation(
            path("/fw/Info plist ü.plist"),
            listOf(
                PlistBuddyCommand.Add(listOf("CFBundleSupportedPlatforms"), PlistValueType.ARRAY),
                PlistBuddyCommand.Add(listOf("CFBundleSupportedPlatforms", "0"), PlistValueType.STRING, "iPhoneOS"),
                PlistBuddyCommand.Set(listOf("MinimumOSVersion"), "15.0"),
                PlistBuddyCommand.Delete(listOf("UIRequiredDeviceCapabilities")),
            ),
        )

        assertEquals(
            listOf(
                "/usr/libexec/PlistBuddy",
                "-c", "Add \":CFBundleSupportedPlatforms\" array",
                "-c", "Add \":CFBundleSupportedPlatforms:0\" string \"iPhoneOS\"",
                "-c", "Set \":MinimumOSVersion\" \"15.0\"",
                "-c", "Delete \":UIRequiredDeviceCapabilities\"",
                "/fw/Info plist ü.plist",
            ),
            invocation.commandLine(),
        )
    }

    @Test
    fun `quotes, backslashes and colons are escaped`() {
        val command = PlistBuddyCommand.Add(listOf("key \"with\" 'quotes'", "a:b"), PlistValueType.STRING, "it's a \"value\" with \\")

        assertEquals(
            """Add ":key \"with\" \'quotes\':a\:b" string "it\'s a \"value\" with \\"""",
            command.render(),
        )
    }

    @Test
    fun `print renders the root or an entry`() {
        assertEquals("Print", PlistBuddyCommand.Print().render())
        assertEquals("Print \":CFBundleVersion\"", PlistBuddyCommand.Print(listOf("CFBundleVersion")).render())
    }

    @Test
    fun `run returns the output`() {
        response = result(stdout = "1.0\n")

        assertEquals("1.0\n", plistBuddy.run(path("/Info.plist"), listOf(PlistBuddyCommand.Print(listOf("CFBundleVersion")))))
    }

    @Test
    fun `run accepts exit code 1 only when failures are ignored`() {
        response = result(stderr = "Delete: Entry, \":missing\", Does Not Exist", exitCode = 1)
        val commands = listOf(PlistBuddyCommand.Delete(listOf("missing")))

        plistBuddy.run(path("/Info.plist"), commands, ignoreFailures = true)
        assertFailsWith<AppleToolException> { plistBuddy.run(path("/Info.plist"), commands) }
    }

    @Test
    fun `invalid commands are rejected`() {
        assertFailsWith<IllegalArgumentException> { plistBuddy.invocation(path("/Info.plist"), emptyList()) }
        assertFailsWith<IllegalArgumentException> { PlistBuddyCommand.Add(emptyList(), PlistValueType.DICT) }
        assertFailsWith<IllegalArgumentException> { PlistBuddyCommand.Add(listOf("a"), PlistValueType.STRING) }
        assertFailsWith<IllegalArgumentException> { PlistBuddyCommand.Set(emptyList(), "v") }
        assertFailsWith<IllegalArgumentException> { PlistBuddyCommand.Delete(emptyList()) }
    }
}
