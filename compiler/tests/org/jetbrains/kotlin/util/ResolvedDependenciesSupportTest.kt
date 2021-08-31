/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.ResolvedDependencies
import org.jetbrains.kotlin.utils.ResolvedDependenciesSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ResolvedDependenciesSupportTest {
    @Test
    fun success1() {
        val originalText = """
        |0 \
        |1 io.ktor:ktor-io,io.ktor:ktor-io-macosx64[1.5.4] #0[1.5.4]
        |${'\t'}/some/path/ktor-io.klib
        |${'\t'}/some/path/ktor-io-cinterop-bits.klib
        |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
        |2 org.jetbrains.kotlin:kotlin-stdlib-common[1.5.0] #1[1.4.32] #3[1.5.0] #4[1.5.0]
        |${'\t'}/some/path/kotlin-stdlib-common-1.5.0.jar
        |3 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64[1.5.0-RC-native-mt] #1[1.4.3-native-mt] #0[1.5.0-RC-native-mt]
        |${'\t'}/some/path/kotlinx-coroutines-core.klib
        |4 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-macosx64[0.16.1] #3[0.16.1] #1[0.15.1]
        |${'\t'}/some/path/atomicfu.klib
        |${'\t'}/some/path/atomicfu-cinterop-interop.klib
        |
        """.trimMargin()

        val (deserializedModules, sourceCodeModuleId) = ResolvedDependenciesSupport.deserialize(originalText) { lineNo, line ->
            fail("Unexpected failure at line $lineNo: $line")
        }
        val restoredText = ResolvedDependenciesSupport.serialize(ResolvedDependencies(deserializedModules, sourceCodeModuleId))

        assertEquals(originalText, restoredText)
    }

    @Test
    fun success2() {
        val originalText = """
        |0 \
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native[1.0] #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        val (deserializedModules, sourceCodeModuleId) = ResolvedDependenciesSupport.deserialize(originalText) { lineNo, line ->
            fail("Unexpected failure at line $lineNo: $line")
        }
        val restoredText = ResolvedDependenciesSupport.serialize(ResolvedDependencies(deserializedModules, sourceCodeModuleId))

        assertEquals(originalText, restoredText)
    }

    @Test
    fun success3() {
        val originalText = """
        |0 \
        |1 org.jetbrains.kotlin:kotlin-stdlib-common[1.5.0] #2[1.4.32] #3[1.5.0] #4[1.5.0] #5[1.4.32] #6[1.4.32] #7[1.4.32] #8[1.4.32]
        |${'\t'}/some/path/kotlin-stdlib-common-1.5.0.jar
        |2 io.ktor:ktor-client-core,io.ktor:ktor-client-core-macosx64[1.5.4] #0[1.5.4]
        |${'\t'}/some/path/ktor-client-core.klib
        |3 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64[1.5.0-RC-native-mt] #0[1.5.0-RC-native-mt] #2[1.4.3-native-mt] #5[1.4.3-native-mt] #6[1.4.3-native-mt] #7[1.4.3-native-mt] #8[1.4.3-native-mt]
        |${'\t'}/some/path/kotlinx-coroutines-core.klib
        |4 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-macosx64[0.16.1] #3[0.16.1] #7[0.15.1] #6[0.15.1] #5[0.15.1] #8[0.15.1] #2[0.15.1]
        |${'\t'}/some/path/atomicfu.klib
        |${'\t'}/some/path/atomicfu-cinterop-interop.klib
        |5 io.ktor:ktor-http,io.ktor:ktor-http-macosx64[1.5.4] #2[1.5.4] #8[1.5.4]
        |${'\t'}/some/path/ktor-http.klib
        |6 io.ktor:ktor-utils,io.ktor:ktor-utils-macosx64[1.5.4] #5[1.5.4]
        |${'\t'}/some/path/ktor-utils.klib
        |${'\t'}/some/path/ktor-utils-cinterop-utils.klib
        |7 io.ktor:ktor-io,io.ktor:ktor-io-macosx64[1.5.4] #6[1.5.4]
        |${'\t'}/some/path/ktor-io.klib
        |${'\t'}/some/path/ktor-io-cinterop-bits.klib
        |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
        |8 io.ktor:ktor-http-cio,io.ktor:ktor-http-cio-macosx64[1.5.4] #2[1.5.4]
        |${'\t'}/some/path/ktor-http-cio.klib
        |
        """.trimMargin()

        val (deserializedModules, sourceCodeModuleId) = ResolvedDependenciesSupport.deserialize(originalText) { lineNo, line ->
            fail("Unexpected failure at line $lineNo: $line")
        }
        val restoredText = ResolvedDependenciesSupport.serialize(ResolvedDependencies(deserializedModules, sourceCodeModuleId))

        assertEquals(originalText, restoredText)
    }

    @Test
    fun success4() {
        val originalText = """
        |0 baz-native,foo,org.sample.bar
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native[1.0] #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        val (deserializedModules, sourceCodeModuleId) = ResolvedDependenciesSupport.deserialize(originalText) { lineNo, line ->
            fail("Unexpected failure at line $lineNo: $line")
        }
        val restoredText = ResolvedDependenciesSupport.serialize(ResolvedDependencies(deserializedModules, sourceCodeModuleId))

        assertEquals(originalText, restoredText)
    }

    @Test(expected = NoSuchElementException::class)
    fun failure1() {
        // There is no record with number 42!
        val originalText = """
        |0 \
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0] #42[42.42]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native[1.0] #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        ResolvedDependenciesSupport.deserialize(originalText) { lineNo, _ ->
            assertEquals(1, lineNo)
            throw MyException()
        }

        fail()
    }

    @Test(expected = MyException::class)
    fun failure2() {
        // Name not specified.
        val originalText = """
        |0 \
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 [1.0] #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        ResolvedDependenciesSupport.deserialize(originalText) { lineNo, _ ->
            assertEquals(3, lineNo)
            throw MyException()
        }

        fail()
    }

    @Test(expected = MyException::class)
    fun failure3() {
        // Version not specified.
        val originalText = """
        |0 \
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        ResolvedDependenciesSupport.deserialize(originalText) { lineNo, _ ->
            assertEquals(3, lineNo)
            throw MyException()
        }

        fail()
    }

    @Test(expected = MyException::class)
    fun failure4() {
        // Source code module ID not specified.
        val originalText = """
        |0
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        ResolvedDependenciesSupport.deserialize(originalText) { lineNo, _ ->
            assertEquals(0, lineNo)
            throw MyException()
        }

        fail()
    }

    @Test(expected = MyException::class)
    fun failure5() {
        // Source code module ID not specified.
        val originalText = """
        |1 org.sample:liba,org.sample:liba-native[2.0] #0[2.0] #2[1.0]
        |${'\t'}/some/path/liba.klib
        |2 org.sample:libb,org.sample:libb-native #0[1.0]
        |${'\t'}/some/path/libb.klib
        |
        """.trimMargin()

        ResolvedDependenciesSupport.deserialize(originalText) { lineNo, _ ->
            assertEquals(0, lineNo)
            throw MyException()
        }

        fail()
    }

    private class MyException : Exception()
}
