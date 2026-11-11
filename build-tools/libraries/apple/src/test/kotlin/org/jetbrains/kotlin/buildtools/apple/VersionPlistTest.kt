/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VersionPlistTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reads string values of an Xcode version plist`() {
        val plist = tempDir.resolve("version.plist").apply { writeText(xcodeVersionPlist("27.2", "27B52")) }

        val values = VersionPlist.read(plist)

        assertEquals("27.2", values["CFBundleShortVersionString"])
        assertEquals("27B52", values["ProductBuildVersion"])
        assertEquals("IDEApplication", values["ProjectName"])
    }

    @Test
    fun `does not fetch the DTD`() {
        // An unreachable URL: parsing would fail or hang if the parser tried to load it.
        val plist = xcodeVersionPlist("16.4", "16F6").replace(
            "http://www.apple.com/DTDs/PropertyList-1.0.dtd",
            "http://127.0.0.1:1/unreachable.dtd",
        )

        val values = VersionPlist.read(plist.byteInputStream())

        assertEquals("16.4", values["CFBundleShortVersionString"])
    }

    @Test
    fun `keeps integer and real values and skips containers and booleans`() {
        val plist = """
            <plist version="1.0"><dict>
              <key>count</key><integer>3</integer>
              <key>ratio</key><real>1.5</real>
              <key>flag</key><true/>
              <key>list</key><array><string>x</string></array>
              <key>name</key><string>ü ñ</string>
            </dict></plist>
        """.trimIndent()

        assertEquals(mapOf("count" to "3", "ratio" to "1.5", "name" to "ü ñ"), VersionPlist.read(plist.byteInputStream()))
    }

    @Test
    fun `a binary plist is reported as malformed with its path`() {
        val plist = tempDir.resolve("Info.plist").apply { writeBytes("bplist00".toByteArray() + ByteArray(16)) }

        val exception = assertFailsWith<VersionPlist.MalformedPlistException> { VersionPlist.read(plist) }

        assertTrue(exception.message!!.startsWith("$plist: not an XML property list"), exception.message)
    }

    @Test
    fun `a root other than plist is malformed`() {
        val exception = assertFailsWith<VersionPlist.MalformedPlistException> {
            VersionPlist.read("<dict/>".byteInputStream())
        }
        assertEquals("the root element is <dict>, not <plist>", exception.message)
    }

    @Test
    fun `a top-level array is malformed`() {
        val exception = assertFailsWith<VersionPlist.MalformedPlistException> {
            VersionPlist.read("<plist><array/></plist>".byteInputStream())
        }
        assertEquals("the top-level value is not a <dict>", exception.message)
    }

    @Test
    fun `a key without a value is malformed`() {
        val exception = assertFailsWith<VersionPlist.MalformedPlistException> {
            VersionPlist.read("<plist><dict><key>a</key></dict></plist>".byteInputStream())
        }
        assertEquals("no value for key 'a'", exception.message)
    }
}

/** A `version.plist` as Xcode 27.2 ships it, with the version and build replaced. */
fun xcodeVersionPlist(version: String, build: String): String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
    	<key>BuildVersion</key>
    	<string>2</string>
    	<key>CFBundleShortVersionString</key>
    	<string>$version</string>
    	<key>CFBundleVersion</key>
    	<string>25400.63.5</string>
    	<key>ProductBuildVersion</key>
    	<string>$build</string>
    	<key>ProjectName</key>
    	<string>IDEApplication</string>
    	<key>SourceVersion</key>
    	<string>25400063005000000</string>
    </dict>
    </plist>
""".trimIndent()
