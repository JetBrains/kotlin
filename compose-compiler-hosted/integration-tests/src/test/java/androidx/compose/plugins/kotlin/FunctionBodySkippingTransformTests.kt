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
            fun Test(x: Int, y: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) 0 else x
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              val y = if (%default and 0b0010 !== 0) 0 else y
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00011000
              } else if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (%dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
                Wrap(restartableFunction(%composer, -756386900, true) { %composer: Composer<N>?, %changed: Int ->
                  if (x > 0) {
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
            fun Wrap(y: Int, children: Function3<@[ParameterName(name = 'x')] Int, Composer<N>, Int, Unit>, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(83839239)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b0010 else 0b0100
              }
              if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(children)) 0b1000 else 0b00010000
              }
              if (%dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
                children(y, %composer, 0b0110 and %dirty)
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Wrap(y, children, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698906)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) 0 else x
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              val y = if (%default and 0b0010 !== 0) 0 else y
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00011000
              } else if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (%dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
                Wrap(10, restartableFunction(%composer, -756386995, true) { it: Int, %composer: Composer<N>?, %changed: Int ->
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
            fun Test(x: Int, y: Int, %composer: Composer<N>?, %changed: Int, %default: Int): Int {
              %composer.startReplaceableGroup(80698815)
              val x = if (%default and 0b0001 !== 0) 0 else x
              val y = if (%default and 0b0010 !== 0) 0 else y
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
            val test: Function3<Int, Composer<N>, Int, Unit> = restartableFunctionInstance(-1071322214, true) { x: Int, %composer: Composer<N>?, %changed: Int ->
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
            fun Test(x: Int, %composer: Composer<N>?, %changed: Int): Int {
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
            fun Test(x: Int, y: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b1000 else 0b00010000
              }
              if (%dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
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
            fun CanSkip(a: Int, b: Foo, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(-73129790)
              var %dirty = %changed
              val a = if (%default and 0b0001 !== 0) 0 else a
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0010 else 0b0100
              }
              val b = if (%default and 0b0010 !== 0) {
                Foo()
              } else {
                b
              }
              if (%default.inv() and 0b0010 !== 0 || %dirty and 0b00101011 xor 0b00101010 !== 0 || !%composer.skipping) {
                print("Hello World")
              } else {
                %composer.skipCurrentGroup()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                CanSkip(a, b, %composer, %changed or 0b0001, %default)
              }
            }
            @Composable
            fun CannotSkip(a: Int, b: Foo, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(-1704897664)
              print("Hello World")
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                CannotSkip(a, b, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun NoParams(%composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(1802049251)
              if (%changed !== 0 || !%composer.skipping) {
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
            fun Test(%composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              if (%changed !== 0 || !%composer.skipping) {
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
            fun Test(x: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun Test(x: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) 0 else x
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun Test(x: Int, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) {
                %composer.startReplaceableGroup(80698833)
                val tmp0_group = I(%composer, 0)
                %composer.endReplaceableGroup()
                tmp0_group
              } else {
                %composer.startReplaceableGroup(80698765)
                %composer.endReplaceableGroup()
                x
              }
              if (%default and 0b0001 === 0 && %changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun Test(x: Foo, %composer: Composer<N>?, %changed: Int) {
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
            fun Test(x: Foo, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              val x = if (%default and 0b0001 !== 0) {
                Foo()
              } else {
                x
              }
              if (%default.inv() and 0b0001 !== 0 || %dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun Test(a: Int, b: Boolean, c: Int, d: Foo, e: List<Int>, %composer: Composer<N>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(80698815)
              var %dirty = %changed
              if (%default and 0b0001 === 0 && %changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0010 else 0b0100
              }
              if (%default and 0b0010 === 0 && %changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b1000 else 0b00010000
              }
              val c = if (%default and 0b0100 !== 0) 0 else c
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b01100000
              } else if (%changed and 0b01100000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b00100000 else 0b01000000
              }
              val d = if (%default and 0b1000 !== 0) {
                Foo()
              } else {
                d
              }
              val e = if (%default and 0b00010000 !== 0) {
                emptyList()
              } else {
                e
              }
              if (%default.inv() and 0b00011000 !== 0 || %dirty and 0b101010101011 xor 0b101010101010 !== 0 || !%composer.skipping) {
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
            fun X(x: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(2777)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun A(x: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
            fun A(x: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
                Provide(restartableFunction(%composer, -756386884, true) { y: Int, %composer: Composer<N>?, %changed: Int ->
                  Provide(restartableFunction(%composer, -756386923, true) { z: Int, %composer: Composer<N>?, %changed: Int ->
                    B(x, y, z, %composer, 0b0110 and %dirty or 0b00011000 and %changed shl 0b0010 or 0b01100000 and %changed shl 0b0100, 0)
                  }, %composer, 0)
                  val tmp0_return = B(x, y, 0, %composer, 0b0110 and %dirty or 0b00011000 and %changed shl 0b0010, 0b0100)
                  tmp0_return
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
            fun A(x: Int, %composer: Composer<N>?, %changed: Int) {
              %composer.startRestartGroup(2064)
              var %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0010 else 0b0100
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
                @Composable
                fun foo(y: Int, %composer: Composer<N>?, %changed: Int) {
                  %composer.startRestartGroup(1906525656)
                  var %dirty = %changed
                  if (%changed and 0b0110 === 0) {
                    %dirty = %dirty or if (%composer.changed(y)) 0b0010 else 0b0100
                  }
                  if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
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
}