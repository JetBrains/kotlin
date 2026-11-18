/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the real tools of the selected Xcode. The test task declares the Xcode selection as an input, so a result from
 * another host or another Xcode is not reused.
 */
@EnabledOnOs(OS.MAC)
class AppleToolsMacosTest {
    @TempDir
    lateinit var tempDir: Path

    private val runner = ProcessAppleToolRunner()
    private lateinit var xcode: XcodeInstallation
    private lateinit var tools: AppleTools

    // No search directories: the tests must not read the user's home. The selected Xcode is found without them.
    private val locator = XcodeLocator(runner, System.getenv(), searchDirectories = emptyList())

    @BeforeEach
    fun setUp() {
        xcode = try {
            locator.resolve()
        } catch (e: XcodeNotFoundException) {
            assumeTrue(false, "No Xcode or Command Line Tools selected: ${e.message}")
            throw e
        }
        tools = AppleTools(xcode, runner)
    }

    private fun swiftPackage(): Path {
        val packageDirectory = tempDir.resolve("Answer Package").createDirectories()
        packageDirectory.resolve("Package.swift").writeText(
            """
            // swift-tools-version:5.9
            import PackageDescription
            let package = Package(
                name: "Answer",
                products: [.library(name: "Answer", targets: ["Answer"])],
                targets: [.target(name: "Answer")]
            )
            """.trimIndent()
        )
        packageDirectory.resolve("Sources/Answer").createDirectories().resolve("Answer.swift").writeText("public let answer = 42\n")
        return packageDirectory
    }

    private fun assumeXcodeApp() = assumeTrue(xcode.kind == XcodeKind.XCODE, "Needs Xcode.app, but $xcode is selected")

    private fun run(invocation: AppleToolInvocation): String = runner.runChecked(invocation).stdout

    private fun compile(arch: String, output: Path, vararg flags: String): Path {
        val file = tempDir.resolve("src ü").createDirectories().resolve("${output.fileName}.c")
            .apply { writeText("int answer(void) { return 42; }\n") }
        run(tools.toolchainInvocation("clang", listOf("-arch", arch, *flags, file.toString(), "-o", output.toString())))
        return output
    }

    @Test
    fun `the selected Xcode matches xcodebuild and is listed`() {
        assumeXcodeApp()

        assertEquals(xcode.version, tools.xcodebuild.version())
        assertEquals(listOf(xcode.appBundle), locator.installations().map { it.appBundle })
    }

    @Test
    fun `tools come from the developer dir of the installation, not from the selection`() {
        assertTrue(tools.xcrun.find("clang").startsWith(xcode.developerDir), "clang is outside ${xcode.developerDir}")

        val commandLineTools = XcodeLocator.installationAt(path("/Library/Developer/CommandLineTools"))
        assumeTrue(commandLineTools != null && commandLineTools != xcode, "Needs Command Line Tools next to the selected Xcode")
        val clang = AppleTools(commandLineTools!!, runner).xcrun.find("clang")
        assertTrue(clang.startsWith(commandLineTools.developerDir), "$clang is outside ${commandLineTools.developerDir}")
    }

    @Test
    fun `xcrun runs a tool with input`() {
        assertEquals("héllo\n", tools.xcrun.runTool("cat", input = "héllo\n").stdout)
    }

    @Test
    fun `xcodebuild accepts showBuildSettings for a Swift package and builds it`() {
        assumeXcodeApp()
        val packageDirectory = swiftPackage()
        val derivedData = tempDir.resolve("Derived Data")
        val destination = ApplePlatform.MACOS.genericDestination

        val settings = tools.xcodebuild.showBuildSettingsInvocation(packageDirectory, scheme = "Answer")
            .let { runner.runChecked(it.copy(arguments = it.arguments + listOf("-destination", destination, "-derivedDataPath", "$derivedData"))) }
        val lines = mutableListOf<String>()
        tools.xcodebuild.build(
            packageDirectory, "Answer", destination,
            actions = listOf("build"), derivedDataPath = derivedData, quiet = true, output = OutputMode.Forward { lines += it },
        )

        // A package scheme has no settings to print, so this only shows that the command is accepted.
        assertTrue("Resolve Package Graph" in settings.stdout.lines(), settings.stdout)
        assertTrue(derivedData.resolve("Build/Products").isDirectory(), "No products in $derivedData; output: $lines")
    }

    @Test
    fun `libtool, lipo and xcodebuild build an XCFramework`() {
        assumeXcodeApp()
        val out = tempDir.resolve("out dir").createDirectories()
        val arm64 = tools.libtool.let {
            val obj = compile("arm64", out.resolve("answer_arm64.o"), "-c")
            out.resolve("libanswer_arm64.a").also { lib -> it.createStatic(listOf(obj), lib) }
        }
        val x64 = tools.libtool.let {
            val obj = compile("x86_64", out.resolve("answer_x64.o"), "-c")
            out.resolve("libanswer_x64.a").also { lib -> it.createStatic(listOf(obj), lib) }
        }
        val fat = out.resolve("libanswer.a")
        tools.lipo.create(listOf(arm64, x64), fat)

        val archs = run(tools.toolchainInvocation("lipo", listOf("-archs", fat.toString())))
        assertEquals(setOf("arm64", "x86_64"), archs.trim().split(" ").toSet())

        val headers = out.resolve("Headers").createDirectories().apply { resolve("answer.h").writeText("int answer(void);\n") }
        val xcframework = out.resolve("Answer ü.xcframework")
        tools.xcodebuild.createXCFramework(listOf(XCFrameworkSlice.Library(fat, headers)), xcframework)

        assertTrue(xcframework.resolve("Info.plist").isRegularFile())
    }

    @Test
    fun `xcodebuild copies debug symbols of a library given by a relative path`() {
        assumeXcodeApp()
        val dylib = compile("arm64", tempDir.resolve("libanswer.dylib"), "-g", "-dynamiclib")
        tools.xcrun.runTool("dsymutil", listOf(dylib.toString()))
        val relativeDsym = java.nio.file.Paths.get("").toAbsolutePath().relativize(tempDir.resolve("libanswer.dylib.dSYM").toAbsolutePath())
        val xcframework = tempDir.resolve("Answer.xcframework")

        tools.xcodebuild.createXCFramework(listOf(XCFrameworkSlice.Library(dylib, debugSymbols = listOf(relativeDsym))), xcframework)

        assertTrue(xcframework.resolve("macos-arm64/dSYMs/libanswer.dylib.dSYM").isDirectory(), "No dSYM in $xcframework")
    }

    @Test
    fun `install_name_tool and codesign work on a dylib`() {
        val dylib = compile("arm64", tempDir.resolve("libanswer.dylib"), "-dynamiclib")

        tools.installNameTool.setId(dylib, "@rpath/Answer.framework/Answer")
        tools.codesign.sign(dylib, "-")

        val installName = run(tools.toolchainInvocation("otool", listOf("-D", dylib.toString()))).lines()[1]
        assertEquals("@rpath/Answer.framework/Answer", installName)
        run(tools.systemInvocation(path("/usr/bin/codesign"), listOf("--verify", dylib.toString())))
    }

    @Test
    fun `PlistBuddy round-trips quotes, backslashes, colons and non-ASCII text`() {
        val plist = tempDir.resolve("Info ü.plist")
        val values = mapOf(
            "plain" to "iPhoneOS",
            "quote\"key" to "it's a \"value\"",
            "colon:key" to "back\\slash",
            "ünïcode" to "日本語 ü",
        )

        tools.plistBuddy.run(plist, values.map { (key, value) -> PlistBuddyCommand.Add(listOf(key), PlistValueType.STRING, value) })

        values.forEach { (key, value) ->
            assertEquals("$value\n", tools.plistBuddy.run(plist, listOf(PlistBuddyCommand.Print(listOf(key)))), "Value of '$key'")
        }
        tools.plistBuddy.run(plist, listOf(PlistBuddyCommand.Set(listOf("colon:key"), "a 'new' value")))
        assertEquals("a 'new' value\n", tools.plistBuddy.run(plist, listOf(PlistBuddyCommand.Print(listOf("colon:key")))))
        tools.plistBuddy.run(plist, listOf(PlistBuddyCommand.Delete(listOf("missing"))), ignoreFailures = true)
    }

    @Test
    fun `plutil converts a plist to JSON`() {
        val plist = tempDir.resolve("project.pbxproj")
        tools.plistBuddy.run(plist, listOf(PlistBuddyCommand.Add(listOf("objectVersion"), PlistValueType.STRING, "77")))
        val json = tempDir.resolve("project.json")

        tools.plutil.convertToJson(plist, json)

        assertEquals("""{"objectVersion":"77"}""", tools.plutil.convertToJson(plist))
        assertTrue(json.isRegularFile())
    }

    @Test
    fun `xattr marks a directory`() {
        val directory = tempDir.resolve("Build dir").createDirectories()

        tools.xattr.markCreatedByBuildSystem(directory)

        val read = tools.systemInvocation(path("/usr/bin/xattr"), listOf("-p", "com.apple.xcode.CreatedByBuildSystem", directory.toString()))
        val value = run(read)
        assertEquals("true", value.trim())
    }

    @Test
    fun `xcrun finds tools and SDKs`() {
        assertTrue(tools.xcrun.find("clang").isRegularFile())
        assertTrue(tools.xcrun.showSdkPath(ApplePlatform.MACOS).isDirectory())
        if (xcode.kind == XcodeKind.XCODE) {
            assertTrue(tools.xcrun.showSdkPath(ApplePlatform.IOS).isDirectory())
            assertTrue(tools.xcrun.showSdkPath(ApplePlatform.IOS_SIMULATOR).isDirectory())
        }
    }

    @Test
    fun `swift package describe returns JSON`() {
        val description = tools.swiftPackage.describe(swiftPackage(), EnvironmentCleanup.SDK_VARIABLES)

        assertTrue(description.trimStart().startsWith("{"), description)
    }

    @Test
    fun `swift resolves and builds a package`() {
        val packageDirectory = swiftPackage()
        val scratch = tempDir.resolve("scratch dir")

        tools.swiftPackage.resolve(packageDirectory, scratchPath = scratch)
        tools.swiftPackage.build(packageDirectory, listOf("--scratch-path", scratch.toString()))
        val binPath = tools.swiftPackage.showBinPath(packageDirectory, listOf("--scratch-path", scratch.toString()))

        assertTrue(binPath.toRealPath().startsWith(scratch.toRealPath()), "$binPath is outside $scratch")
        assertTrue(binPath.isDirectory(), "$binPath does not exist")
    }

    @Test
    fun `simctl lists runtimes and devices`() {
        assumeXcodeApp()

        assertTrue(tools.simctl.listRuntimes().trimStart().startsWith("{"))
        assertEquals("== Devices ==", tools.simctl.listDevices().lineSequence().first())
        assertTrue(tools.simctl.listDevices(json = true).trimStart().startsWith("{"))
    }
}
