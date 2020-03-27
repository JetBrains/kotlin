/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.junit.Test

class DefaultParamTransformTests : AbstractIrTransformTest() {
    private fun defaultParams(
        unchecked: String,
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        ComposeTransforms.DEFAULT xor
        ComposeTransforms.FRAMED_CLASSES xor
        ComposeTransforms.CALLS_AND_EMITS xor
        ComposeTransforms.RESTART_GROUPS or
        ComposeTransforms.CONTROL_FLOW_GROUPS or
        ComposeTransforms.FUNCTION_BODY_SKIPPING,
        """
            import androidx.compose.Composable

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.Composable

            $unchecked
        """.trimIndent(),
        dumpTree
    )

    @Test
    fun testComposableWithAndWithoutDefaultParams(): Unit = defaultParams(
        """
            @Composable fun A(x: Int) { }
            @Composable fun B(x: Int = 1) { }
        """,
        """
            @Composable
            fun Test() {
                A(1)
                B()
                B(2)
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              if (%changed !== 0 || !%composer.skipping) {
                A(1, %composer, 0b0110)
                B(0, %composer, 0, 0b0001)
                B(2, %composer, 0b0110, 0)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testParameterHoles(): Unit = defaultParams(
        """
            @Composable fun A(a: Int = 0, b: Int = 1, c: Int = 2, d: Int = 3, e: Int = 4) { }
        """,
        """
            @Composable
            fun Test() {
                A(0, 1, 2)
                A(a = 0, c = 2)
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              if (%changed !== 0 || !%composer.skipping) {
                A(0, 1, 2, 0, 0, %composer, 0b01111110, 0b00011000)
                A(0, 0, 2, 0, 0, %composer, 0b01100110, 0b00011010)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testNonStaticDefaultExpressions(): Unit = defaultParams(
        """
            fun makeInt(): Int = 123
        """,
        """
            @Composable
            fun Test(x: Int = makeInt()) {

            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) {
                makeInt()
              } else {
                x
              }
              if (%default and 0b0001 === 0 && %changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testEarlierParameterReferences(): Unit = defaultParams(
        """
        """,
        """
            @Composable
            fun A(a: Int = 0, b: Int = a + 1) {
                print(a)
                print(b)
            }
        """,
        """
            @Composable
            fun A(a: Int, b: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              val a = if (%default and 0b0001 !== 0) 0 else a
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0010 else 0b0100
              }
              val b = if (%default and 0b0010 !== 0) {
                a + 1
              } else {
                b
              }
              if (%default and 0b0010 === 0 && %changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b1000 else 0b00010000
              }
              if (%dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
                print(a)
                print(b)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                A(a, b, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )
}