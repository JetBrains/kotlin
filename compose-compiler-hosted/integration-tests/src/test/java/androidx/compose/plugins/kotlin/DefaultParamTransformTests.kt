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
        """
            import androidx.compose.Composable
            import androidx.compose.ComposableContract

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.Composable
            import androidx.compose.ComposableContract

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
            fun Test(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              if (%changed !== 0 || !%composer.skipping) {
                A(1, %composer, <>, 0b0110)
                B(0, %composer, <>, 0, 0b0001)
                B(2, %composer, <>, 0b0110, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Test(%composer, %key, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testInlineClassDefaultParameter(): Unit = defaultParams(
        """
            inline class Foo(val value: Int)
        """,
        """
            @Composable
            fun Example(foo: Foo = Foo(0)) {
                print(foo)
            }
            @Composable
            fun Test() {
                Example()
            }
        """,
        """
            @Composable
            fun Example(foo: Foo, %composer: Composer<*>?, %key: Int, %changed: Int, %default: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val foo = foo
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo.value)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  foo = Foo(0)
                }
                print(foo)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(foo, %composer, %key, %changed or 0b0001, %default)
              }
            }
            @Composable
            fun Test(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              if (%changed !== 0 || !%composer.skipping) {
                Example(Foo(0), %composer, <>, 0, 0b0001)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Test(%composer, %key, %changed or 0b0001)
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
            fun Test(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              if (%changed !== 0 || !%composer.skipping) {
                A(0, 1, 2, 0, 0, %composer, <>, 0b01111110, 0b00011000)
                A(0, 0, 2, 0, 0, %composer, <>, 0b01100110, 0b00011010)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Test(%composer, %key, %changed or 0b0001)
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
            fun Test(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int, %default: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val x = x
              if (%default and 0b0001 === 0 && %changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  %composer.startDefaults()
                  if (%default and 0b0001 !== 0) {
                    x = makeInt()
                  }
                  %composer.endDefaults()
                } else {
                  %composer.skipCurrentGroup()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Test(x, %composer, %key, %changed or 0b0001, %default)
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
            fun A(a: Int, b: Int, %composer: Composer<*>?, %key: Int, %changed: Int, %default: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val a = a
              val b = b
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 === 0 && %changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00010000 else 0b1000
              }
              if (%dirty and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  %composer.startDefaults()
                  if (%default and 0b0001 !== 0) {
                    a = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    b = a + 1
                  }
                  %composer.endDefaults()
                } else {
                  %composer.skipCurrentGroup()
                }
                print(a)
                print(b)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                A(a, b, %composer, %key, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun test30Parameters(): Unit = defaultParams(
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
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0
            ) {
                print("Hello world!")
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, %composer: Composer<*>?, %key: Int, %changed: Int, %changed1: Int, %changed2: Int, %default: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val a00 = a00
              val a01 = a01
              val a02 = a02
              val a03 = a03
              val a04 = a04
              val a05 = a05
              val a06 = a06
              val a07 = a07
              val a08 = a08
              val a09 = a09
              val a10 = a10
              val a11 = a11
              val a12 = a12
              val a13 = a13
              val a14 = a14
              val a15 = a15
              val a16 = a16
              val a17 = a17
              val a18 = a18
              val a19 = a19
              val a20 = a20
              val a21 = a21
              val a22 = a22
              val a23 = a23
              val a24 = a24
              val a25 = a25
              val a26 = a26
              val a27 = a27
              val a28 = a28
              val a29 = a29
              val a30 = a30
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00011000
              } else if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00010000 else 0b1000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b01100000
              } else if (%changed and 0b01100000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b01000000 else 0b00100000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b000110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b011000000000
              } else if (%changed and 0b011000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b0001100000000000
              } else if (%changed and 0b0001100000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b0110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000
              } else if (%changed and 0b00011000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000
              } else if (%changed and 0b01100000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b000110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty = %dirty or 0b011000000000000000000000
              } else if (%changed and 0b011000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a10)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty = %dirty or 0b0001100000000000000000000000
              } else if (%changed and 0b0001100000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a11)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b0110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a12)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000000000000000
              } else if (%changed and 0b00011000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a13)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000000000000000
              } else if (%changed and 0b01100000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a14)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b0110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b0100 else 0b0010
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000
              } else if (%changed1 and 0b00011000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b00010000 else 0b1000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000
              } else if (%changed1 and 0b01100000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b01000000 else 0b00100000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b000110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000
              } else if (%changed1 and 0b011000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000
              } else if (%changed1 and 0b0001100000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a20)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b0110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a21)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000
              } else if (%changed1 and 0b00011000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a22)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000
              } else if (%changed1 and 0b01100000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a23)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b000110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a24)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000000000000000
              } else if (%changed1 and 0b011000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a25)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000000000000000
              } else if (%changed1 and 0b0001100000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a26)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b0110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a27)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000000000000000
              } else if (%changed1 and 0b00011000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a28)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000000000000000
              } else if (%changed1 and 0b01100000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a29)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b0110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%dirty and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty1 and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty2 and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                if (%default and 0b1000000000000000 !== 0) {
                  a15 = 0
                }
                if (%default and 0b00010000000000000000 !== 0) {
                  a16 = 0
                }
                if (%default and 0b00100000000000000000 !== 0) {
                  a17 = 0
                }
                if (%default and 0b01000000000000000000 !== 0) {
                  a18 = 0
                }
                if (%default and 0b10000000000000000000 !== 0) {
                  a19 = 0
                }
                if (%default and 0b000100000000000000000000 !== 0) {
                  a20 = 0
                }
                if (%default and 0b001000000000000000000000 !== 0) {
                  a21 = 0
                }
                if (%default and 0b010000000000000000000000 !== 0) {
                  a22 = 0
                }
                if (%default and 0b100000000000000000000000 !== 0) {
                  a23 = 0
                }
                if (%default and 0b0001000000000000000000000000 !== 0) {
                  a24 = 0
                }
                if (%default and 0b0010000000000000000000000000 !== 0) {
                  a25 = 0
                }
                if (%default and 0b0100000000000000000000000000 !== 0) {
                  a26 = 0
                }
                if (%default and 0b1000000000000000000000000000 !== 0) {
                  a27 = 0
                }
                if (%default and 0b00010000000000000000000000000000 !== 0) {
                  a28 = 0
                }
                if (%default and 0b00100000000000000000000000000000 !== 0) {
                  a29 = 0
                }
                if (%default and 0b01000000000000000000000000000000 !== 0) {
                  a30 = 0
                }
                print("Hello world!")
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, %composer, %key, %changed or 0b0001, %changed1, %changed2, %default)
              }
            }
        """
    )

    @Test
    fun test31Parameters(): Unit = defaultParams(
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
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0,
                a31: Int = 0
            ) {
                print("Hello world!")
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, a31: Int, %composer: Composer<*>?, %key: Int, %changed: Int, %changed1: Int, %changed2: Int, %default: Int, %default1: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val a00 = a00
              val a01 = a01
              val a02 = a02
              val a03 = a03
              val a04 = a04
              val a05 = a05
              val a06 = a06
              val a07 = a07
              val a08 = a08
              val a09 = a09
              val a10 = a10
              val a11 = a11
              val a12 = a12
              val a13 = a13
              val a14 = a14
              val a15 = a15
              val a16 = a16
              val a17 = a17
              val a18 = a18
              val a19 = a19
              val a20 = a20
              val a21 = a21
              val a22 = a22
              val a23 = a23
              val a24 = a24
              val a25 = a25
              val a26 = a26
              val a27 = a27
              val a28 = a28
              val a29 = a29
              val a30 = a30
              val a31 = a31
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00011000
              } else if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00010000 else 0b1000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b01100000
              } else if (%changed and 0b01100000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b01000000 else 0b00100000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b000110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b011000000000
              } else if (%changed and 0b011000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b0001100000000000
              } else if (%changed and 0b0001100000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b0110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000
              } else if (%changed and 0b00011000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000
              } else if (%changed and 0b01100000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b000110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty = %dirty or 0b011000000000000000000000
              } else if (%changed and 0b011000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a10)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty = %dirty or 0b0001100000000000000000000000
              } else if (%changed and 0b0001100000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a11)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b0110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a12)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000000000000000
              } else if (%changed and 0b00011000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a13)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000000000000000
              } else if (%changed and 0b01100000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a14)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b0110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b0100 else 0b0010
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000
              } else if (%changed1 and 0b00011000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b00010000 else 0b1000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000
              } else if (%changed1 and 0b01100000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b01000000 else 0b00100000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b000110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000
              } else if (%changed1 and 0b011000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000
              } else if (%changed1 and 0b0001100000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a20)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b0110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a21)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000
              } else if (%changed1 and 0b00011000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a22)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000
              } else if (%changed1 and 0b01100000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a23)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b000110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a24)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000000000000000
              } else if (%changed1 and 0b011000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a25)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000000000000000
              } else if (%changed1 and 0b0001100000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a26)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b0110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a27)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000000000000000
              } else if (%changed1 and 0b00011000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a28)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000000000000000
              } else if (%changed1 and 0b01100000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a29)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b0110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%default1 and 0b0001 !== 0) {
                %dirty2 = %dirty2 or 0b00011000
              } else if (%changed2 and 0b00011000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a31)) 0b00010000 else 0b1000
              }
              if (%dirty and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty1 and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty2 and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                if (%default and 0b1000000000000000 !== 0) {
                  a15 = 0
                }
                if (%default and 0b00010000000000000000 !== 0) {
                  a16 = 0
                }
                if (%default and 0b00100000000000000000 !== 0) {
                  a17 = 0
                }
                if (%default and 0b01000000000000000000 !== 0) {
                  a18 = 0
                }
                if (%default and 0b10000000000000000000 !== 0) {
                  a19 = 0
                }
                if (%default and 0b000100000000000000000000 !== 0) {
                  a20 = 0
                }
                if (%default and 0b001000000000000000000000 !== 0) {
                  a21 = 0
                }
                if (%default and 0b010000000000000000000000 !== 0) {
                  a22 = 0
                }
                if (%default and 0b100000000000000000000000 !== 0) {
                  a23 = 0
                }
                if (%default and 0b0001000000000000000000000000 !== 0) {
                  a24 = 0
                }
                if (%default and 0b0010000000000000000000000000 !== 0) {
                  a25 = 0
                }
                if (%default and 0b0100000000000000000000000000 !== 0) {
                  a26 = 0
                }
                if (%default and 0b1000000000000000000000000000 !== 0) {
                  a27 = 0
                }
                if (%default and 0b00010000000000000000000000000000 !== 0) {
                  a28 = 0
                }
                if (%default and 0b00100000000000000000000000000000 !== 0) {
                  a29 = 0
                }
                if (%default and 0b01000000000000000000000000000000 !== 0) {
                  a30 = 0
                }
                if (%default1 and 0b0001 !== 0) {
                  a31 = 0
                }
                print("Hello world!")
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, %composer, %key, %changed or 0b0001, %changed1, %changed2, %default, %default1)
              }
            }
        """
    )

    @Test
    fun test31ParametersWithSomeUnstable(): Unit = defaultParams(
        """
            class Foo
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
                a09: Foo = Foo(),
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0,
                a31: Foo = Foo()
            ) {
                print("Hello world!")
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Foo?, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, a31: Foo?, %composer: Composer<*>?, %key: Int, %changed: Int, %changed1: Int, %changed2: Int, %default: Int, %default1: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val a00 = a00
              val a01 = a01
              val a02 = a02
              val a03 = a03
              val a04 = a04
              val a05 = a05
              val a06 = a06
              val a07 = a07
              val a08 = a08
              val a09 = a09
              val a10 = a10
              val a11 = a11
              val a12 = a12
              val a13 = a13
              val a14 = a14
              val a15 = a15
              val a16 = a16
              val a17 = a17
              val a18 = a18
              val a19 = a19
              val a20 = a20
              val a21 = a21
              val a22 = a22
              val a23 = a23
              val a24 = a24
              val a25 = a25
              val a26 = a26
              val a27 = a27
              val a28 = a28
              val a29 = a29
              val a30 = a30
              val a31 = a31
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00011000
              } else if (%changed and 0b00011000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00010000 else 0b1000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b01100000
              } else if (%changed and 0b01100000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b01000000 else 0b00100000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b000110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b011000000000
              } else if (%changed and 0b011000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b0001100000000000
              } else if (%changed and 0b0001100000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b0110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000
              } else if (%changed and 0b00011000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000
              } else if (%changed and 0b01100000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b10000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty = %dirty or 0b011000000000000000000000
              } else if (%changed and 0b011000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a10)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty = %dirty or 0b0001100000000000000000000000
              } else if (%changed and 0b0001100000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a11)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b0110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a12)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty = %dirty or 0b00011000000000000000000000000000
              } else if (%changed and 0b00011000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a13)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty = %dirty or 0b01100000000000000000000000000000
              } else if (%changed and 0b01100000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a14)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b0110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b0100 else 0b0010
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000
              } else if (%changed1 and 0b00011000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b00010000 else 0b1000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000
              } else if (%changed1 and 0b01100000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b01000000 else 0b00100000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b000110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000
              } else if (%changed1 and 0b011000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b010000000000 else 0b001000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000
              } else if (%changed1 and 0b0001100000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a20)) 0b0001000000000000 else 0b100000000000
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b0110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a21)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000
              } else if (%changed1 and 0b00011000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a22)) 0b00010000000000000000 else 0b1000000000000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000
              } else if (%changed1 and 0b01100000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a23)) 0b01000000000000000000 else 0b00100000000000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b000110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a24)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b011000000000000000000000
              } else if (%changed1 and 0b011000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a25)) 0b010000000000000000000000 else 0b001000000000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0001100000000000000000000000
              } else if (%changed1 and 0b0001100000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a26)) 0b0001000000000000000000000000 else 0b100000000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b0110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a27)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00011000000000000000000000000000
              } else if (%changed1 and 0b00011000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a28)) 0b00010000000000000000000000000000 else 0b1000000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b01100000000000000000000000000000
              } else if (%changed1 and 0b01100000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a29)) 0b01000000000000000000000000000000 else 0b00100000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b0110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%default1 and 0b0001 !== 0) {
                %dirty2 = %dirty2 or 0b1000
              }
              if (%default.inv() and 0b001000000000 !== 0 || %default1.inv() and 0b0001 !== 0 || %dirty and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty1 and 0b00101010101010101010101010101011 xor 0b00101010101010101010101010101010 !== 0 || %dirty2 and 0b1011 xor 0b1010 !== 0 || !%composer.skipping) {
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  %composer.startDefaults()
                  if (%default and 0b0001 !== 0) {
                    a00 = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    a01 = 0
                  }
                  if (%default and 0b0100 !== 0) {
                    a02 = 0
                  }
                  if (%default and 0b1000 !== 0) {
                    a03 = 0
                  }
                  if (%default and 0b00010000 !== 0) {
                    a04 = 0
                  }
                  if (%default and 0b00100000 !== 0) {
                    a05 = 0
                  }
                  if (%default and 0b01000000 !== 0) {
                    a06 = 0
                  }
                  if (%default and 0b10000000 !== 0) {
                    a07 = 0
                  }
                  if (%default and 0b000100000000 !== 0) {
                    a08 = 0
                  }
                  if (%default and 0b001000000000 !== 0) {
                    a09 = Foo()
                  }
                  if (%default and 0b010000000000 !== 0) {
                    a10 = 0
                  }
                  if (%default and 0b100000000000 !== 0) {
                    a11 = 0
                  }
                  if (%default and 0b0001000000000000 !== 0) {
                    a12 = 0
                  }
                  if (%default and 0b0010000000000000 !== 0) {
                    a13 = 0
                  }
                  if (%default and 0b0100000000000000 !== 0) {
                    a14 = 0
                  }
                  if (%default and 0b1000000000000000 !== 0) {
                    a15 = 0
                  }
                  if (%default and 0b00010000000000000000 !== 0) {
                    a16 = 0
                  }
                  if (%default and 0b00100000000000000000 !== 0) {
                    a17 = 0
                  }
                  if (%default and 0b01000000000000000000 !== 0) {
                    a18 = 0
                  }
                  if (%default and 0b10000000000000000000 !== 0) {
                    a19 = 0
                  }
                  if (%default and 0b000100000000000000000000 !== 0) {
                    a20 = 0
                  }
                  if (%default and 0b001000000000000000000000 !== 0) {
                    a21 = 0
                  }
                  if (%default and 0b010000000000000000000000 !== 0) {
                    a22 = 0
                  }
                  if (%default and 0b100000000000000000000000 !== 0) {
                    a23 = 0
                  }
                  if (%default and 0b0001000000000000000000000000 !== 0) {
                    a24 = 0
                  }
                  if (%default and 0b0010000000000000000000000000 !== 0) {
                    a25 = 0
                  }
                  if (%default and 0b0100000000000000000000000000 !== 0) {
                    a26 = 0
                  }
                  if (%default and 0b1000000000000000000000000000 !== 0) {
                    a27 = 0
                  }
                  if (%default and 0b00010000000000000000000000000000 !== 0) {
                    a28 = 0
                  }
                  if (%default and 0b00100000000000000000000000000000 !== 0) {
                    a29 = 0
                  }
                  if (%default and 0b01000000000000000000000000000000 !== 0) {
                    a30 = 0
                  }
                  if (%default1 and 0b0001 !== 0) {
                    a31 = Foo()
                  }
                  %composer.endDefaults()
                } else {
                  %composer.skipCurrentGroup()
                }
                print("Hello world!")
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, %composer, %key, %changed or 0b0001, %changed1, %changed2, %default, %default1)
              }
            }
        """
    )

    @Test
    fun testDefaultArgsForFakeOverridesSuperMethods(): Unit = defaultParams(
        """
        """,
        """
            open class Foo {
                @ComposableContract(restartable = false) @Composable fun foo(x: Int = 0) {}
            }
            class Bar: Foo() {
                @ComposableContract(restartable = false) @Composable fun Example() {
                    foo()
                }
            }
        """,
        """
            open class Foo {
              @ComposableContract(restartable = false)
              @Composable
              fun foo(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int, %default: Int) {
                %composer.startReplaceableGroup(%key)
                val x = if (%default and 0b0001 !== 0) 0 else x
                %composer.endReplaceableGroup()
              }
            }
            class Bar : Foo {
              @ComposableContract(restartable = false)
              @Composable
              fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
                %composer.startReplaceableGroup(%key)
                foo(0, %composer, <>, 0, 0b0001)
                %composer.endReplaceableGroup()
              }
            }
        """
    )
}