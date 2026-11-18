/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Command lines of the tools with one or two operations: lipo, libtool, install_name_tool, codesign, plutil, xattr, xcrun. */
class ToolCommandLinesTest {
    private var response = result()
    private val runner = RecordingAppleToolRunner { response }
    private val tools = AppleTools(fakeXcode, runner)

    @Test
    fun `lipo create`() {
        tools.lipo.create(listOf(path("/out/arm64/lib ü.a"), path("/out/x64/lib.a")), path("/out/fat/lib.a"))

        assertEquals(
            listOf(XCRUN, "lipo", "-create", "/out/arm64/lib ü.a", "/out/x64/lib.a", "-output", "/out/fat/lib.a"),
            runner.single.commandLine(),
        )
    }

    @Test
    fun `lipo needs an input`() {
        val exception = assertFailsWith<IllegalArgumentException> { tools.lipo.create(emptyList(), path("/out.a")) }
        assertEquals("lipo -create needs at least one input", exception.message)
    }

    @Test
    fun `libtool static`() {
        tools.libtool.createStatic(listOf(path("/obj/a.o"), path("/obj/b c.a")), path("/out/libmerged.a"))

        assertEquals(
            listOf(XCRUN, "libtool", "-static", "-o", "/out/libmerged.a", "/obj/a.o", "/obj/b c.a"),
            runner.single.commandLine(),
        )
    }

    @Test
    fun `libtool needs an input`() {
        assertFailsWith<IllegalArgumentException> { tools.libtool.createStatic(emptyList(), path("/out.a")) }
    }

    @Test
    fun `install_name_tool sets the id`() {
        tools.installNameTool.setId(path("/fat/Shared.framework/Shared"), "@rpath/Shared.framework/Shared")

        assertEquals(
            listOf(XCRUN, "install_name_tool", "-id", "@rpath/Shared.framework/Shared", "/fat/Shared.framework/Shared"),
            runner.single.commandLine(),
        )
        assertFailsWith<IllegalArgumentException> { tools.installNameTool.setIdInvocation(path("/x"), "") }
    }

    @Test
    fun `codesign signs with force by default and ends options with a double dash`() {
        tools.codesign.sign(path("/App.app/Frameworks/-weird name.framework"), "Apple Development: Jane (ABC)")

        assertEquals(
            listOf("/usr/bin/codesign", "--force", "--sign", "Apple Development: Jane (ABC)", "--", "/App.app/Frameworks/-weird name.framework"),
            runner.single.commandLine(),
        )
        assertEquals(
            listOf("/usr/bin/codesign", "--sign", "-", "--", "/bin.dylib"),
            tools.codesign.signInvocation(path("/bin.dylib"), "-", force = false).commandLine(),
        )
        assertFailsWith<IllegalArgumentException> { tools.codesign.signInvocation(path("/x"), " ") }
    }

    @Test
    fun `plutil converts to stdout or to a file`() {
        response = result(stdout = "{}")

        assertEquals("{}", tools.plutil.convertToJson(path("/X.xcodeproj/project.pbxproj")))
        assertEquals(
            listOf("/usr/bin/plutil", "-convert", "json", "-o", "-", "/X.xcodeproj/project.pbxproj"),
            runner.single.commandLine(),
        )
        assertEquals(
            listOf("/usr/bin/plutil", "-convert", "json", "-o", "/out/project.json", "/X.xcodeproj/project.pbxproj"),
            tools.plutil.convertToJsonInvocation(path("/X.xcodeproj/project.pbxproj"), path("/out/project.json")).commandLine(),
        )
    }

    @Test
    fun `xattr marks a directory as created by the build system`() {
        tools.xattr.markCreatedByBuildSystem(path("/DerivedData/Build dir"))

        assertEquals(
            listOf("/usr/bin/xattr", "-w", "com.apple.xcode.CreatedByBuildSystem", "true", "/DerivedData/Build dir"),
            runner.single.commandLine(),
        )
    }

    @Test
    fun `xcrun finds tools and SDKs`() {
        response = result(stdout = "/Applications/Xcode 16.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/ld\n")

        assertEquals(
            path("/Applications/Xcode 16.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/ld"),
            tools.xcrun.find("ld"),
        )
        assertEquals(listOf(XCRUN, "--find", "ld"), runner.single.commandLine())
        assertEquals(
            listOf(XCRUN, "--sdk", "watchsimulator", "--show-sdk-path"),
            tools.xcrun.showSdkPathInvocation(ApplePlatform.WATCHOS_SIMULATOR).commandLine(),
        )
        assertFailsWith<IllegalArgumentException> { tools.xcrun.findInvocation("") }
    }

    @Test
    fun `xcrun runs tools without a wrapper and passes input`() {
        response = result(stdout = "formatted")

        val formatted = tools.xcrun.runTool("xcprojformatter", input = "{\"objects\":{}}")

        assertEquals("formatted", formatted.stdout)
        assertEquals(listOf(XCRUN, "xcprojformatter"), runner.single.commandLine())
        assertEquals("{\"objects\":{}}", runner.single.input)
        assertEquals(
            listOf(XCRUN, "otool", "-D", "/lib ü.dylib"),
            tools.xcrun.toolInvocation("otool", listOf("-D", "/lib ü.dylib"), workingDirectory = path("/work")).commandLine(),
        )
        assertFailsWith<IllegalArgumentException> { tools.xcrun.toolInvocation(" ") }
    }

    @Test
    fun `a failing tool throws with its command line`() {
        response = result(stderr = "fatal error: lipo: can't open input file", exitCode = 1)

        val exception = assertFailsWith<AppleToolException> { tools.lipo.create(listOf(path("/missing.a")), path("/out.a")) }

        assertEquals(
            "Command failed with exit code 1: DEVELOPER_DIR='/Applications/Xcode 16.app/Contents/Developer' " +
                    "/usr/bin/xcrun lipo -create /missing.a -output /out.a\nfatal error: lipo: can't open input file",
            exception.message,
        )
    }
}
