/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
@DisabledOnOs(OS.WINDOWS, disabledReason = "Uses symbolic links")
class XcodeLocatorTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var applications: Path
    private lateinit var userApplications: Path

    /** What `xcode-select -p` prints; `null` makes it fail. */
    private var xcodeSelectPath: Path? = null

    /** What `mdfind` prints; `null` makes it fail. */
    private var spotlightResult: List<Path>? = emptyList()

    private val runner = RecordingAppleToolRunner { invocation ->
        when (invocation.executable.toString()) {
            "/usr/bin/xcode-select" -> xcodeSelectPath?.let { result(stdout = "$it\n") } ?: result(stderr = "error", exitCode = 2)
            "/usr/bin/mdfind" -> spotlightResult?.let { result(stdout = it.joinToString("\n")) } ?: result(exitCode = 1)
            else -> error("Unexpected command: $invocation")
        }
    }

    @BeforeEach
    fun setUp() {
        // Real paths, so expectations match the symlink-resolved paths the locator reports (macOS /var is a link).
        val root = tempDir.toRealPath()
        applications = root.resolve("Applications").createDirectories()
        userApplications = root.resolve("home/user/Applications").createDirectories()
    }

    private fun locator(
        environment: Map<String, String> = emptyMap(),
        useSpotlight: Boolean = false,
    ) = XcodeLocator(runner, environment, listOf(applications, userApplications), useSpotlight)

    private fun xcodeBundle(parent: Path, name: String, version: String, build: String): Path {
        val bundle = parent.resolve(name)
        bundle.resolve("Contents/Developer/usr/bin").createDirectories().resolve("xcodebuild").createFile()
        bundle.resolve("Contents/version.plist").writeText(xcodeVersionPlist(version, build))
        return bundle
    }

    private fun commandLineTools(): Path =
        tempDir.toRealPath().resolve("Library/Developer/CommandLineTools").apply { resolve("usr/bin").createDirectories() }

    private fun xcode(bundle: Path, version: XcodeVersion, build: String) =
        XcodeInstallation(bundle.resolve("Contents/Developer"), bundle, XcodeKind.XCODE, version, build)

    // Selection

    @Test
    fun `DEVELOPER_DIR may name the bundle or its Contents Developer`() {
        val bundle = xcodeBundle(applications, "Xcode-16.4.0.app", "16.4", "16F6")
        val expected = xcode(bundle, XcodeVersion(16, 4), "16F6")

        assertEquals(expected, locator(mapOf("DEVELOPER_DIR" to bundle.toString())).resolve())
        assertEquals(expected, locator(mapOf("DEVELOPER_DIR" to "$bundle/Contents/Developer")).resolve())
        assertTrue(runner.invocations.isEmpty(), "xcode-select must not run when DEVELOPER_DIR is set")
    }

    @Test
    fun `DEVELOPER_DIR may name Command Line Tools`() {
        val clt = commandLineTools()

        val selected = locator(mapOf("DEVELOPER_DIR" to clt.toString())).resolve()

        assertEquals(XcodeInstallation(clt, null, XcodeKind.COMMAND_LINE_TOOLS, null, null), selected)
    }

    @Test
    fun `DEVELOPER_DIR that is not an installation is an error`() {
        val missing = tempDir.resolve("Missing.app")

        val exception = assertFailsWith<XcodeNotFoundException> { locator(mapOf("DEVELOPER_DIR" to missing.toString())).resolve() }

        assertEquals("DEVELOPER_DIR is '$missing', which is not an Xcode or Command Line Tools installation", exception.message)
    }

    @Test
    fun `without DEVELOPER_DIR the xcode-select choice is used`() {
        val bundle = xcodeBundle(applications, "Xcode-26.0.0.app", "26.0", "17A324")
        xcodeSelectPath = bundle.resolve("Contents/Developer")

        val selected = locator(mapOf("DEVELOPER_DIR" to " ")).resolve()

        assertEquals(xcode(bundle, XcodeVersion(26), "17A324"), selected)
        val xcodeSelect = runner.single
        assertEquals(listOf("/usr/bin/xcode-select", "-p"), xcodeSelect.commandLine())
        assertEquals(EnvironmentCleanup(names = setOf("DEVELOPER_DIR")), xcodeSelect.environmentCleanup)
    }

    @Test
    fun `a failing xcode-select is an error with a fix`() {
        val exception = assertFailsWith<XcodeNotFoundException> { locator().resolve() }

        assertEquals(
            "No Xcode is selected: 'xcode-select -p' failed (error). Install Xcode, then run " +
                    "'sudo xcode-select --switch <path to Xcode.app>' or set DEVELOPER_DIR.",
            exception.message,
        )
    }

    @Test
    fun `a missing xcode-select is an error with the reason`() {
        val locator = XcodeLocator(
            { throw AppleToolUnavailableException("Cannot start '/usr/bin/xcode-select -p': No such file or directory") },
            emptyMap(),
            listOf(applications),
        )

        val exception = assertFailsWith<XcodeNotFoundException> { locator.resolve() }

        assertEquals(
            "No Xcode is selected: Cannot start '/usr/bin/xcode-select -p': No such file or directory. Install Xcode, then run " +
                    "'sudo xcode-select --switch <path to Xcode.app>' or set DEVELOPER_DIR.",
            exception.message,
        )
        assertEquals(emptyList(), locator.installations())
    }

    @Test
    fun `an xcode-select path that is not an installation is an error`() {
        xcodeSelectPath = tempDir.resolve("gone/Contents/Developer")

        val exception = assertFailsWith<XcodeNotFoundException> { locator().resolve() }

        assertEquals(
            "'xcode-select -p' prints '$xcodeSelectPath', which is not an Xcode or Command Line Tools installation",
            exception.message,
        )
    }

    // Explicit developer dir

    @Test
    fun `an explicit developer dir wins over DEVELOPER_DIR`() {
        val requested = xcodeBundle(userApplications, "Xcode beta ü.app", "27.0", "27A5")
        val fromEnvironment = xcodeBundle(applications, "Xcode.app", "26.0", "17A324")

        val resolved = locator(mapOf("DEVELOPER_DIR" to fromEnvironment.toString())).resolve(XcodeRequest.DeveloperDir(requested))

        assertEquals(xcode(requested, XcodeVersion(27), "27A5"), resolved)
    }

    @Test
    fun `an explicit developer dir that is not an installation is an error`() {
        val notXcode = tempDir.resolve("Empty.app").createDirectories()

        val exception = assertFailsWith<XcodeNotFoundException> { locator().resolve(XcodeRequest.DeveloperDir(notXcode)) }

        assertEquals("'$notXcode' is not an Xcode or Command Line Tools installation", exception.message)
    }

    // Listing installations

    @Test
    fun `installations lists xcodes-style bundles, highest version first`() {
        val older = xcodeBundle(applications, "Xcode-16.4.0.app", "16.4", "16F6")
        val newer = xcodeBundle(applications, "Xcode-27.2.0.app", "27.2", "27B52")
        val patch = xcodeBundle(userApplications, "Xcode-16.4.1.app", "16.4.1", "16F7")

        assertEquals(
            listOf(
                xcode(newer, XcodeVersion(27, 2), "27B52"),
                xcode(patch, XcodeVersion(16, 4, 1), "16F7"),
                xcode(older, XcodeVersion(16, 4), "16F6"),
            ),
            locator().installations(),
        )
    }

    @Test
    fun `a symlink to a listed bundle counts once and a broken symlink is skipped`() {
        val real = xcodeBundle(applications, "Xcode-27.2.0.app", "27.2", "27B52")
        Files.createSymbolicLink(applications.resolve("Xcode.app"), real)
        val removed = xcodeBundle(applications, "Xcode-26.3.0.app", "26.3", "17C52")
        Files.createSymbolicLink(applications.resolve("Xcode-old.app"), removed)
        removed.deleteRecursively()

        assertEquals(listOf(xcode(real, XcodeVersion(27, 2), "27B52")), locator().installations())
    }

    @Test
    fun `equal versions prefer the earlier search directory`() {
        val user = xcodeBundle(userApplications, "A.app", "16.4", "16F6")
        val system = xcodeBundle(applications, "Z.app", "16.4", "16F6")

        assertEquals(listOf(system, user), locator().installations().map { it.appBundle })
    }

    @Test
    fun `bundles without xcodebuild, without version plist or with a broken one are not Xcode`() {
        applications.resolve("Simulator.app/Contents").createDirectories()
        xcodeBundle(applications, "NoTools.app", "16.4", "16F6").resolve("Contents/Developer/usr/bin/xcodebuild").deleteRecursively()
        xcodeBundle(applications, "Broken.app", "16.4", "16F6").resolve("Contents/version.plist").writeText("<plist><array/></plist>")
        applications.resolve("NotADirectory.app").createFile()

        assertEquals(emptyList(), locator().installations())
    }

    @Test
    fun `the selected Xcode is listed even outside the search directories`() {
        val elsewhere = xcodeBundle(tempDir.toRealPath().resolve("ci/xcode_16.4_16F6"), "Xcode.app", "16.4", "16F6")
        xcodeSelectPath = elsewhere.resolve("Contents/Developer")

        assertEquals(listOf(xcode(elsewhere, XcodeVersion(16, 4), "16F6")), locator().installations())
    }

    @Test
    fun `Spotlight is not asked by default`() {
        locator().installations()

        assertEquals(listOf("/usr/bin/xcode-select"), runner.invocations.map { it.executable.toString() })
    }

    @Test
    fun `Spotlight adds bundles outside the search directories when enabled`() {
        val found = xcodeBundle(tempDir.toRealPath().resolve("Volumes/External"), "Xcode-15.4.0.app", "15.4", "15F31d")
        spotlightResult = listOf(found)

        val installations = locator(useSpotlight = true).installations()

        assertEquals(listOf(xcode(found, XcodeVersion(15, 4), "15F31d")), installations)
        val mdfind = runner.invocations.single { it.executable.toString() == "/usr/bin/mdfind" }
        assertEquals(listOf("kMDItemCFBundleIdentifier == 'com.apple.dt.Xcode'"), mdfind.arguments)
    }

    @Test
    fun `a failing Spotlight falls back to the directory scan`() {
        val bundle = xcodeBundle(applications, "Xcode.app", "16.4", "16F6")
        spotlightResult = null

        assertEquals(listOf(bundle), locator(useSpotlight = true).installations().map { it.appBundle })
    }

    // Version requests

    @Test
    fun `a version prefix picks the highest match`() {
        xcodeBundle(applications, "Xcode-16.2.0.app", "16.2", "16C5032a")
        val highest16 = xcodeBundle(applications, "Xcode-16.4.1.app", "16.4.1", "16F7")
        val exact = xcodeBundle(applications, "Xcode-16.4.0.app", "16.4", "16F6")
        xcodeBundle(applications, "Xcode-26.0.0.app", "26.0", "17A324")

        assertEquals(highest16, locator().resolve(XcodeRequest.Version("16")).appBundle)
        assertEquals(highest16, locator().resolve(XcodeRequest.Version("16.4")).appBundle)
        assertEquals(exact, locator().resolve(XcodeRequest.Version("16.4.0")).appBundle)
    }

    @Test
    fun `a build number must match exactly`() {
        xcodeBundle(applications, "Xcode-16.4.1.app", "16.4.1", "16F7")
        val beta = xcodeBundle(applications, "Xcode-beta.app", "16.4", "16F5a")

        assertEquals(beta, locator().resolve(XcodeRequest.Version("16F5a")).appBundle)
        assertFailsWith<XcodeNotFoundException> { locator().resolve(XcodeRequest.Version("16F5")) }
    }

    @Test
    fun `no match lists what was found`() {
        val bundle = xcodeBundle(applications, "Xcode-16.4.0.app", "16.4", "16F6")

        val exception = assertFailsWith<XcodeNotFoundException> { locator().resolve(XcodeRequest.Version("15")) }

        assertEquals("No installed Xcode matches '15'. Found:\n  Xcode 16.4 (16F6) at $bundle", exception.message)
    }

    @Test
    fun `no match without any installation names the search directories`() {
        val exception = assertFailsWith<XcodeNotFoundException> { locator().resolve(XcodeRequest.Version("16.4")) }

        assertEquals(
            "No installed Xcode matches '16.4'. No Xcode installations were found in $applications, $userApplications.",
            exception.message,
        )
    }

    @Test
    fun `an invalid spec is rejected when the request is created`() {
        val exception = assertFailsWith<IllegalArgumentException> { XcodeRequest.Version("latest") }
        assertEquals("'latest' is neither an Xcode version such as 16.4 nor a build number such as 16F6", exception.message)
        listOf("", " 16.4", "16.4.1.0", "16.", "16.4-beta", "Xcode 16.4", "16f6", "16.0 Beta 3").forEach {
            assertFailsWith<IllegalArgumentException>("'$it' should be rejected") { XcodeRequest.Version(it) }
        }
    }

    @Test
    fun `a bundle without a readable version never matches a version request`() {
        val bundle = xcodeBundle(applications, "Xcode-odd.app", "16.4 (beta)", "16F6")

        assertEquals(listOf(XcodeInstallation(bundle.resolve("Contents/Developer"), bundle, XcodeKind.XCODE, null, "16F6")), locator().installations())
        assertFailsWith<XcodeNotFoundException> { locator().resolve(XcodeRequest.Version("16")) }
        assertEquals(bundle, locator().resolve(XcodeRequest.Version("16F6")).appBundle)
    }

    @Test
    fun `a symlink to a developer dir is described by its target`() {
        val bundle = xcodeBundle(applications, "Xcode-16.4.0.app", "16.4", "16F6")
        val link = Files.createSymbolicLink(tempDir.resolve("current"), bundle.resolve("Contents/Developer"))
        val expected = xcode(bundle, XcodeVersion(16, 4), "16F6")

        assertEquals(expected, XcodeLocator.installationAt(link))
        assertEquals(expected, locator(mapOf("DEVELOPER_DIR" to link.toString())).resolve())
    }

    @Test
    fun `a developer dir in a bundle without version plist is not an installation`() {
        val bundle = xcodeBundle(applications, "Stripped.app", "16.4", "16F6")
        bundle.resolve("Contents/version.plist").deleteRecursively()

        assertNull(XcodeLocator.installationAt(bundle.resolve("Contents/Developer")))
    }

    @Test
    fun `Spotlight results that are already listed or not Xcode change nothing`() {
        val bundle = xcodeBundle(applications, "Xcode.app", "16.4", "16F6")
        spotlightResult = listOf(bundle, tempDir.resolve("Gone.app"), applications.resolve("Other.app").createDirectories())

        assertEquals(listOf(bundle), locator(useSpotlight = true).installations().map { it.appBundle })
    }

    @Test
    fun `a bundle inside a nested search directory takes the first matching directory`() {
        val nested = applications.resolve("Xcodes").createDirectories()
        val inNested = xcodeBundle(nested, "A.app", "16.4", "16F6")
        val inUser = xcodeBundle(userApplications, "A.app", "16.4", "16F6")

        val installations = XcodeLocator(runner, emptyMap(), listOf(userApplications, nested, applications)).installations()

        assertEquals(listOf(inUser, inNested), installations.map { it.appBundle })
    }

    // Single installations

    @Test
    fun `installationAt recognises provisioned copies regardless of the bundle name`() {
        val provisioned = xcodeBundle(tempDir.toRealPath(), "xcode_16.4_16F6", "16.4", "16F6")

        assertEquals(xcode(provisioned, XcodeVersion(16, 4), "16F6"), XcodeLocator.installationAt(provisioned))
    }

    @Test
    fun `installationAt returns null for directories that are no installation`() {
        assertNull(XcodeLocator.installationAt(tempDir.resolve("missing")))
        assertNull(XcodeLocator.installationAt(tempDir.resolve("empty").createDirectories()))
    }

    @Test
    fun `installationAt treats a developer dir outside a bundle as Command Line Tools`() {
        val clt = commandLineTools()

        assertEquals(XcodeKind.COMMAND_LINE_TOOLS, XcodeLocator.installationAt(clt)?.kind)
    }

    @Test
    fun `default search directories come from the environment`() {
        assertEquals(
            listOf(path("/Applications"), path("/Users/me", "Applications"), path("/opt/xcodes")),
            XcodeLocator.defaultSearchDirectories(mapOf("HOME" to "/Users/me", "XCODES_DIRECTORY" to "/opt/xcodes")),
        )
        assertEquals(
            listOf(path("/Applications")),
            XcodeLocator.defaultSearchDirectories(mapOf("HOME" to "", "XCODES_DIRECTORY" to "/Applications")),
        )
    }
}
