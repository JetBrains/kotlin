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

class FunctionBodySkippingTransformTests : AbstractIrTransformTest() {
    private fun comparisonPropagation(
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
    fun testIfInLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
            @Composable fun Wrap(children: @Composable() () -> Unit) {
                children()
            }
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                Wrap {
                    if (x > 0) {
                        A(x)
                    } else {
                        A(x)
                    }
                }
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (!EQEQEQ(%default and 0b0001, 0)) 0 else x
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              val y = if (!EQEQEQ(%default and 0b0010, 0)) 0 else y
              if (!EQEQEQ(%default and 0b0010, 0)) {
                %dirty = %dirty or 0b00011000
              } else if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (!EQEQEQ(%dirty and 0b1011 xor 0b1010, 0) || !%composer.skipping) {
                Wrap(restartableFunction(%composer, -756386900, true) { %composer: Composer<*>?, %changed: Int ->
                  if (greater(x, 0)) {
                    %composer.startReplaceableGroup(-447710431)
                    A(x, 0, %composer, 0b0110 and %dirty, 0b0010)
                    %composer.endReplaceableGroup()
                  } else {
                    %composer.startReplaceableGroup(-447710397)
                    A(x, 0, %composer, 0b0110 and %dirty, 0b0010)
                    %composer.endReplaceableGroup()
                  }
                }, %composer, 0)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, y, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableWithAndWithoutDefaultParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable fun Wrap(y: Int, children: @Composable() (x: Int) -> Unit) {
                children(y)
            }
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                Wrap(10) {
                    A(x)
                }
            }
        """,
        """
            @Composable
            fun Wrap(y: Int, children: Function3<@[ParameterName(name = 'x')] Int, Composer<*>, Int, Unit>, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(83839239)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(y)) 0b0010 else 0b0100
              }
              if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(children)) 0b1000 else 0b00010000
              }
              if (!EQEQEQ(%dirty and 0b1011 xor 0b1010, 0) || !%composer.skipping) {
                children(y, %composer, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Wrap(y, children, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698906)
              var %dirty = %changed
              val x = if (!EQEQEQ(%default and 0b0001, 0)) 0 else x
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              val y = if (!EQEQEQ(%default and 0b0010, 0)) 0 else y
              if (!EQEQEQ(%default and 0b0010, 0)) {
                %dirty = %dirty or 0b00011000
              } else if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (!EQEQEQ(%dirty and 0b1011 xor 0b1010, 0) || !%composer.skipping) {
                Wrap(10, restartableFunction(%composer, -756386995, true) { it: Int, %composer: Composer<*>?, %changed: Int ->
                  A(x, 0, %composer, 0b0110 and %dirty, 0b0010)
                }, %composer, 0b0110)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, y, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableWithReturnValue(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0): Int {
                A(x, y)
                return x + y
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer<*>?, %changed: Int, %default: Int): Int {
              %composer.startReplaceableGroup(80698815)
              val x = if (!EQEQEQ(%default and 0b0001, 0)) 0 else x
              val y = if (!EQEQEQ(%default and 0b0010, 0)) 0 else y
              A(x, y, %composer, 0b0110 and %changed or 0b00011000 and %changed, 0)
              val tmp0 = x + y
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testComposableLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            val test = @Composable { x: Int ->
                A(x)
            }
        """,
        """
            val test: Function3<Int, Composer<*>, Int, Unit> = restartableFunctionInstance(-1071322214, true) { x: Int, %composer: Composer<*>?, %changed: Int ->
              A(x, 0, %composer, 0b0110 and %changed, 0b0010)
            }
        """
    )

    @Test
    fun testComposableFunExprBody(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int) = A()
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(80698815)
              val tmp0 = A(0, 0, %composer, 0, 0b0011)
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testParamReordering(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int, y: Int) {
                A(y = y, x = x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (!EQEQEQ(%dirty and 0b1011 xor 0b1010, 0) || !%composer.skipping) {
                val tmp0_y = y
                val tmp1_x = x
                A(x, y, %composer, 0b0110 and %dirty or 0b00011000 and %dirty, 0)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, y, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testStableUnstableParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo
        """,
        """
            @Composable fun CanSkip(a: Int = 0, b: Foo = Foo()) {
                print("Hello World")
            }
            @Composable fun CannotSkip(a: Int, b: Foo) {
                print("Hello World")
            }
            @Composable fun NoParams() {
                print("Hello World")
            }
        """,
        """
            @Composable
            fun CanSkip(a: Int, b: Foo, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(-73129790)
              var %dirty = %changed
              val a = if (!EQEQEQ(%default and 0b0001, 0)) 0 else a
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0010 else 0b0100
              }
              val b = if (!EQEQEQ(%default and 0b0010, 0)) {
                Foo()
              } else {
                b
              }
              if (!EQEQEQ(%default.inv() and 0b0010, 0) || !EQEQEQ(%dirty and 0b1011 xor 0b1010, 0) || !%composer.skipping) {
                print("Hello World")
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                CanSkip(a, b, %composer, %changed or 0b0001, %default)
              }
            }
            @Composable
            fun CannotSkip(a: Int, b: Foo, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(-1704897664)
              print("Hello World")
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                CannotSkip(a, b, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun NoParams(%composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(1802049251)
              if (!EQEQEQ(%changed, 0) || !%composer.skipping) {
                print("Hello World")
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                NoParams(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testNoParams(): Unit = comparisonPropagation(
        """
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test() {
                A()
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              if (!EQEQEQ(%changed, 0) || !%composer.skipping) {
                A(%composer, 0)
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
    fun testSingleStableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                A(x, %composer, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testSingleStableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (!EQEQEQ(%default and 0b0001, 0)) 0 else x
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                A(x, %composer, 0b0110 and %dirty)
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
    fun testSingleStableParamWithComposableDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
            @Composable fun I(): Int { return 10 }
        """,
        """
            @Composable
            fun Test(x: Int = I()) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (!EQEQEQ(%default and 0b0001, 0)) {
                %composer.startReplaceableGroup(80698833)
                val tmp0_group = I(%composer, 0)
                %composer.endReplaceableGroup()
                tmp0_group
              } else {
                %composer.startReplaceableGroup(80698765)
                %composer.endReplaceableGroup()
                x
              }
              if (EQEQEQ(%default and 0b0001, 0) && EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                A(x, %composer, 0b0110 and %dirty)
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
    fun testSingleUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              A(x, %composer, 0b0110 and %changed)
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testSingleUnstableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo
        """,
        """
            @Composable
            fun Test(x: Foo = Foo()) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (!EQEQEQ(%default and 0b0001, 0)) {
                Foo()
              } else {
                x
              }
              if (!EQEQEQ(%default.inv() and 0b0001, 0) || !EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                A(x, %composer, 0b0110 and %dirty)
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
    fun testManyNonOptionalParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(a: Int, b: Boolean, c: Int, d: Foo, e: List<Int>) {}
            class Foo
        """,
        """
            @Composable
            fun Test(a: Int, b: Boolean, c: Int = 0, d: Foo = Foo(), e: List<Int> = emptyList()) {
                A(a, b, c, d, e)
            }
        """,
        """
            @Composable
            fun Test(a: Int, b: Boolean, c: Int, d: Foo, e: List<Int>, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (EQEQEQ(%default and 0b0001, 0) && EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0010 else 0b0100
              }
              if (EQEQEQ(%default and 0b0010, 0) && EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(b)) 0b1000 else 0b00010000
              }
              val c = if (!EQEQEQ(%default and 0b0100, 0)) 0 else c
              if (!EQEQEQ(%default and 0b0100, 0)) {
                %dirty = %dirty or 0b01100000
              } else if (EQEQEQ(%changed and 0b01100000, 0)) {
                %dirty = %dirty or if (%composer.changed(c)) 0b00100000 else 0b01000000
              }
              val d = if (!EQEQEQ(%default and 0b1000, 0)) {
                Foo()
              } else {
                d
              }
              val e = if (!EQEQEQ(%default and 0b00010000, 0)) {
                emptyList()
              } else {
                e
              }
              if (!EQEQEQ(%default.inv() and 0b00011000, 0) || !EQEQEQ(%dirty and 0b001010101011 xor 0b001010101010, 0) || !%composer.skipping) {
                A(a, b, c, d, e, %composer, 0b0110 and %dirty or 0b00011000 and %dirty or 0b01100000 and %dirty or 0b000110000000 and %dirty or 0b011000000000 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Test(a, b, c, d, e, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testRecursiveCall(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun X(x: Int) {
                X(x + 1)
                X(x)
            }
        """,
        """
            @Composable
            fun X(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(2777)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                X(x + 1, %composer, 0)
                X(x, %composer, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                X(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testDifferentParameters(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int, c: Int, d: Int) {}
            val fooGlobal = 10
        """,
        """
            @Composable
            fun A(x: Int) {
                B(
                    // direct parameter
                    x,
                    // transformation
                    x + 1,
                    // literal
                    123,
                    // expression with no parameter
                    fooGlobal
                )
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                B(x, x + 1, 123, fooGlobal, %composer, 0b01100000 or 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testNestedCalls(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int = 0, b: Int = 0, c: Int = 0) {}
            @Composable fun Provide(children: @Composable() (Int) -> Unit) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                Provide { y ->
                    Provide { z ->
                        B(x, y, z)
                    }
                    B(x, y)
                }
                B(x)
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                Provide(restartableFunction(%composer, -756386884, true) { y: Int, %composer: Composer<*>?, %changed: Int ->
                  Provide(restartableFunction(%composer, -756386923, true) { z: Int, %composer: Composer<*>?, %changed: Int ->
                    B(x, y, z, %composer, 0b0110 and %dirty or 0b00011000 and %changed shl 0b0010 or 0b01100000 and %changed shl 0b0100, 0)
                  }, %composer, 0)
                  B(x, y, 0, %composer, 0b0110 and %dirty or 0b00011000 and %changed shl 0b0010, 0b0100)
                }, %composer, 0)
                B(x, 0, 0, %composer, 0b0110 and %dirty, 0b0110)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testLocalFunction(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                @Composable fun foo(y: Int) {
                    B(x, y)
                }
                foo(x)
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                @Composable
                fun foo(y: Int, %composer: Composer<*>?, %changed: Int) {
                  %composer.startRestartGroup(1906525656)
                  var %dirty = %changed
                  if (EQEQEQ(%changed and 0b0110, 0)) {
                    %dirty = %dirty or if (%composer.changed(y)) 0b0010 else 0b0100
                  }
                  if (!EQEQEQ(%dirty and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                    B(x, y, %composer, 0b0110 and %dirty or 0b00011000 and %dirty shl 0b0010)
                  } else {
                    %composer.skipCurrentGroup()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                    foo(y, %composer, %changed or 0b0001)
                  }
                }
                foo(x, %composer, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun test15Parameters(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14
                )
                // in opposite order
                Example(
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(2002223143)
              var %dirty = %changed
              val a00 = if (!EQEQEQ(%default and 0b0001, 0)) 0 else a00
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0010 else 0b0100
              }
              val a01 = if (!EQEQEQ(%default and 0b0010, 0)) 0 else a01
              if (!EQEQEQ(%default and 0b0010, 0)) {
                %dirty = %dirty or 0b00011000
              } else if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b1000 else 0b00010000
              }
              val a02 = if (!EQEQEQ(%default and 0b0100, 0)) 0 else a02
              if (!EQEQEQ(%default and 0b0100, 0)) {
                %dirty = %dirty or 0b01100000
              } else if (EQEQEQ(%changed and 0b01100000, 0)) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b00100000 else 0b01000000
              }
              val a03 = if (!EQEQEQ(%default and 0b1000, 0)) 0 else a03
              if (!EQEQEQ(%default and 0b1000, 0)) {
                %dirty = %dirty or 0b000110000000
              } else if (EQEQEQ(%changed and 0b000110000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b10000000 else 0b000100000000
              }
              val a04 = if (!EQEQEQ(%default and 0b00010000, 0)) 0 else a04
              if (!EQEQEQ(%default and 0b00010000, 0)) {
                %dirty = %dirty or 0b011000000000
              } else if (EQEQEQ(%changed and 0b011000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b001000000000 else 0b010000000000
              }
              val a05 = if (!EQEQEQ(%default and 0b00100000, 0)) 0 else a05
              if (!EQEQEQ(%default and 0b00100000, 0)) {
                %dirty = %dirty or 0b0001100000000000
              } else if (EQEQEQ(%changed and 0b0001100000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b100000000000 else 0b0001000000000000
              }
              val a06 = if (!EQEQEQ(%default and 0b01000000, 0)) 0 else a06
              if (!EQEQEQ(%default and 0b01000000, 0)) {
                %dirty = %dirty or 0b0110000000000000
              } else if (EQEQEQ(%changed and 0b0110000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b0010000000000000 else 0b0100000000000000
              }
              val a07 = if (!EQEQEQ(%default and 0b10000000, 0)) 0 else a07
              if (!EQEQEQ(%default and 0b10000000, 0)) {
                %dirty = %dirty or 0b00011000000000000000
              } else if (EQEQEQ(%changed and 0b00011000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b1000000000000000 else 0b00010000000000000000
              }
              val a08 = if (!EQEQEQ(%default and 0b000100000000, 0)) 0 else a08
              if (!EQEQEQ(%default and 0b000100000000, 0)) {
                %dirty = %dirty or 0b01100000000000000000
              } else if (EQEQEQ(%changed and 0b01100000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b00100000000000000000 else 0b01000000000000000000
              }
              val a09 = if (!EQEQEQ(%default and 0b001000000000, 0)) 0 else a09
              if (!EQEQEQ(%default and 0b001000000000, 0)) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (EQEQEQ(%changed and 0b000110000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b10000000000000000000 else 0b000100000000000000000000
              }
              val a10 = if (!EQEQEQ(%default and 0b010000000000, 0)) 0 else a10
              if (!EQEQEQ(%default and 0b010000000000, 0)) {
                %dirty = %dirty or 0b011000000000000000000000
              } else if (EQEQEQ(%changed and 0b011000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a10)) 0b001000000000000000000000 else 0b010000000000000000000000
              }
              val a11 = if (!EQEQEQ(%default and 0b100000000000, 0)) 0 else a11
              if (!EQEQEQ(%default and 0b100000000000, 0)) {
                %dirty = %dirty or 0b0001100000000000000000000000
              } else if (EQEQEQ(%changed and 0b0001100000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a11)) 0b100000000000000000000000 else 0b0001000000000000000000000000
              }
              val a12 = if (!EQEQEQ(%default and 0b0001000000000000, 0)) 0 else a12
              if (!EQEQEQ(%default and 0b0001000000000000, 0)) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (EQEQEQ(%changed and 0b0110000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a12)) 0b0010000000000000000000000000 else 0b0100000000000000000000000000
              }
              val a13 = if (!EQEQEQ(%default and 0b0010000000000000, 0)) 0 else a13
              if (!EQEQEQ(%default and 0b0010000000000000, 0)) {
                %dirty = %dirty or 0b00011000000000000000000000000000
              } else if (EQEQEQ(%changed and 0b00011000000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a13)) 0b1000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              val a14 = if (!EQEQEQ(%default and 0b0100000000000000, 0)) 0 else a14
              if (!EQEQEQ(%default and 0b0100000000000000, 0)) {
                %dirty = %dirty or 0b01100000000000000000000000000000
              } else if (EQEQEQ(%changed and 0b01100000000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a14)) 0b00100000000000000000000000000000 else 0b01000000000000000000000000000000
              }
              if (!EQEQEQ(%dirty and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010, 0) || !%composer.skipping) {
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, %composer, 0b0110 and %dirty or 0b00011000 and %dirty or 0b01100000 and %dirty or 0b000110000000 and %dirty or 0b011000000000 and %dirty or 0b0001100000000000 and %dirty or 0b0110000000000000 and %dirty or 0b00011000000000000000 and %dirty or 0b01100000000000000000 and %dirty or 0b000110000000000000000000 and %dirty or 0b011000000000000000000000 and %dirty or 0b0001100000000000000000000000 and %dirty or 0b0110000000000000000000000000 and %dirty or 0b00011000000000000000000000000000 and %dirty or 0b01100000000000000000000000000000 and %dirty, 0)
                Example(a14, a13, a12, a11, a10, a09, a08, a07, a06, a05, a04, a03, a02, a01, a00, %composer, 0b0110 and %dirty shr 0b00011100 or 0b00011000 and %dirty shr 0b00011000 or 0b01100000 and %dirty shr 0b00010100 or 0b000110000000 and %dirty shr 0b00010000 or 0b011000000000 and %dirty shr 0b1100 or 0b0001100000000000 and %dirty shr 0b1000 or 0b0110000000000000 and %dirty shr 0b0100 or 0b00011000000000000000 and %dirty or 0b01100000000000000000 and %dirty shl 0b0100 or 0b000110000000000000000000 and %dirty shl 0b1000 or 0b011000000000000000000000 and %dirty shl 0b1100 or 0b0001100000000000000000000000 and %dirty shl 0b00010000 or 0b0110000000000000000000000000 and %dirty shl 0b00010100 or 0b00011000000000000000000000000000 and %dirty shl 0b00011000 or 0b01100000000000000000000000000000 and %dirty shl 0b00011100, 0)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun test16Parameters(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14,
                    a15
                )
                // in opposite order
                Example(
                    a15,
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, %composer: Composer<*>?, %changed: Int, %changed1: Int, %default: Int) {
              %composer.startRestartGroup(2002223143)
              var %dirty = %changed
              var %dirty1 = %changed1
              val a00 = if (!EQEQEQ(%default and 0b0001, 0)) 0 else a00
              if (!EQEQEQ(%default and 0b0001, 0)) {
                %dirty = %dirty or 0b0110
              } else if (EQEQEQ(%changed and 0b0110, 0)) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0010 else 0b0100
              }
              val a01 = if (!EQEQEQ(%default and 0b0010, 0)) 0 else a01
              if (!EQEQEQ(%default and 0b0010, 0)) {
                %dirty = %dirty or 0b00011000
              } else if (EQEQEQ(%changed and 0b00011000, 0)) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b1000 else 0b00010000
              }
              val a02 = if (!EQEQEQ(%default and 0b0100, 0)) 0 else a02
              if (!EQEQEQ(%default and 0b0100, 0)) {
                %dirty = %dirty or 0b01100000
              } else if (EQEQEQ(%changed and 0b01100000, 0)) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b00100000 else 0b01000000
              }
              val a03 = if (!EQEQEQ(%default and 0b1000, 0)) 0 else a03
              if (!EQEQEQ(%default and 0b1000, 0)) {
                %dirty = %dirty or 0b000110000000
              } else if (EQEQEQ(%changed and 0b000110000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b10000000 else 0b000100000000
              }
              val a04 = if (!EQEQEQ(%default and 0b00010000, 0)) 0 else a04
              if (!EQEQEQ(%default and 0b00010000, 0)) {
                %dirty = %dirty or 0b011000000000
              } else if (EQEQEQ(%changed and 0b011000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b001000000000 else 0b010000000000
              }
              val a05 = if (!EQEQEQ(%default and 0b00100000, 0)) 0 else a05
              if (!EQEQEQ(%default and 0b00100000, 0)) {
                %dirty = %dirty or 0b0001100000000000
              } else if (EQEQEQ(%changed and 0b0001100000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b100000000000 else 0b0001000000000000
              }
              val a06 = if (!EQEQEQ(%default and 0b01000000, 0)) 0 else a06
              if (!EQEQEQ(%default and 0b01000000, 0)) {
                %dirty = %dirty or 0b0110000000000000
              } else if (EQEQEQ(%changed and 0b0110000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b0010000000000000 else 0b0100000000000000
              }
              val a07 = if (!EQEQEQ(%default and 0b10000000, 0)) 0 else a07
              if (!EQEQEQ(%default and 0b10000000, 0)) {
                %dirty = %dirty or 0b00011000000000000000
              } else if (EQEQEQ(%changed and 0b00011000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b1000000000000000 else 0b00010000000000000000
              }
              val a08 = if (!EQEQEQ(%default and 0b000100000000, 0)) 0 else a08
              if (!EQEQEQ(%default and 0b000100000000, 0)) {
                %dirty = %dirty or 0b01100000000000000000
              } else if (EQEQEQ(%changed and 0b01100000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b00100000000000000000 else 0b01000000000000000000
              }
              val a09 = if (!EQEQEQ(%default and 0b001000000000, 0)) 0 else a09
              if (!EQEQEQ(%default and 0b001000000000, 0)) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (EQEQEQ(%changed and 0b000110000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b10000000000000000000 else 0b000100000000000000000000
              }
              val a10 = if (!EQEQEQ(%default and 0b010000000000, 0)) 0 else a10
              if (!EQEQEQ(%default and 0b010000000000, 0)) {
                %dirty = %dirty or 0b011000000000000000000000
              } else if (EQEQEQ(%changed and 0b011000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a10)) 0b001000000000000000000000 else 0b010000000000000000000000
              }
              val a11 = if (!EQEQEQ(%default and 0b100000000000, 0)) 0 else a11
              if (!EQEQEQ(%default and 0b100000000000, 0)) {
                %dirty = %dirty or 0b0001100000000000000000000000
              } else if (EQEQEQ(%changed and 0b0001100000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a11)) 0b100000000000000000000000 else 0b0001000000000000000000000000
              }
              val a12 = if (!EQEQEQ(%default and 0b0001000000000000, 0)) 0 else a12
              if (!EQEQEQ(%default and 0b0001000000000000, 0)) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (EQEQEQ(%changed and 0b0110000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a12)) 0b0010000000000000000000000000 else 0b0100000000000000000000000000
              }
              val a13 = if (!EQEQEQ(%default and 0b0010000000000000, 0)) 0 else a13
              if (!EQEQEQ(%default and 0b0010000000000000, 0)) {
                %dirty = %dirty or 0b00011000000000000000000000000000
              } else if (EQEQEQ(%changed and 0b00011000000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a13)) 0b1000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              val a14 = if (!EQEQEQ(%default and 0b0100000000000000, 0)) 0 else a14
              if (!EQEQEQ(%default and 0b0100000000000000, 0)) {
                %dirty = %dirty or 0b01100000000000000000000000000000
              } else if (EQEQEQ(%changed and 0b01100000000000000000000000000000, 0)) {
                %dirty = %dirty or if (%composer.changed(a14)) 0b00100000000000000000000000000000 else 0b01000000000000000000000000000000
              }
              val a15 = if (!EQEQEQ(%default and 0b1000000000000000, 0)) 0 else a15
              if (!EQEQEQ(%default and 0b1000000000000000, 0)) {
                %dirty1 = %dirty1 or 0b0110
              } else if (EQEQEQ(%changed1 and 0b0110, 0)) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b0010 else 0b0100
              }
              if (!EQEQEQ(%dirty and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010, 0) || !EQEQEQ(%dirty1 and 0b0011 xor 0b0010, 0) || !%composer.skipping) {
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, %composer, 0, 0b0110 and %dirty or 0b00011000 and %dirty or 0b01100000 and %dirty or 0b000110000000 and %dirty or 0b011000000000 and %dirty or 0b0001100000000000 and %dirty or 0b0110000000000000 and %dirty or 0b00011000000000000000 and %dirty or 0b01100000000000000000 and %dirty or 0b000110000000000000000000 and %dirty or 0b011000000000000000000000 and %dirty or 0b0001100000000000000000000000 and %dirty or 0b0110000000000000000000000000 and %dirty or 0b00011000000000000000000000000000 and %dirty or 0b01100000000000000000000000000000 and %dirty, 0b0110 and %dirty1)
                Example(a15, a14, a13, a12, a11, a10, a09, a08, a07, a06, a05, a04, a03, a02, a01, a00, %composer, 0, 0b0110 and %dirty1 or 0b00011000 and %dirty shr 0b00011010 or 0b01100000 and %dirty shr 0b00010110 or 0b000110000000 and %dirty shr 0b00010010 or 0b011000000000 and %dirty shr 0b1110 or 0b0001100000000000 and %dirty shr 0b1010 or 0b0110000000000000 and %dirty shr 0b0110 or 0b00011000000000000000 and %dirty shr 0b0010 or 0b01100000000000000000 and %dirty shl 0b0010 or 0b000110000000000000000000 and %dirty shl 0b0110 or 0b011000000000000000000000 and %dirty shl 0b1010 or 0b0001100000000000000000000000 and %dirty shl 0b1110 or 0b0110000000000000000000000000 and %dirty shl 0b00010010 or 0b00011000000000000000000000000000 and %dirty shl 0b00010110 or 0b01100000000000000000000000000000 and %dirty shl 0b00011010, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, %composer, %changed or 0b0001, %changed1, %default)
              }
            }
        """
    )
}