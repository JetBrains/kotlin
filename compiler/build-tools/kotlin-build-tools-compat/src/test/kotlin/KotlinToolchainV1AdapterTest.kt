import org.jetbrains.kotlin.buildtools.internal.compat.fixForFirCheck
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class KotlinToolchainsV1AdapterTest {
    @Test
    fun fixForFirCheck_noChange() {
        val argumentsList = ["abc", "def", "ghi"]
        assertEquals(argumentsList, argumentsList.fixForFirCheck())
    }

    @Test
    fun fixForFirCheck_languageVersion() {
        val argumentsList = ["abc", "-language-version", "1.9", "def"]
        assertEquals(["abc", "-language-version=1.9", "def"], argumentsList.fixForFirCheck())

        val argumentsList2 = ["abc", "def", "-language-version", "1.9"]
        assertEquals(["abc", "def", "-language-version=1.9"], argumentsList2.fixForFirCheck())
    }

    @Test
    fun fixForFirCheck_xFirIc() {
        val argumentsList = ["abc", "-Xuse-fir-ic=true", "def"]
        assertEquals(["abc", "-Xuse-fir-ic", "def"], argumentsList.fixForFirCheck())

        val argumentsList2 = ["abc", "def", "-Xuse-fir-ic=true"]
        assertEquals(["abc", "def", "-Xuse-fir-ic"], argumentsList2.fixForFirCheck())
    }
}
