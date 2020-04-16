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

class ComposerParamTransformTests : AbstractIrTransformTest() {
    fun composerParam(
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        ComposeTransforms.COMPOSER_PARAM,
        """
            import androidx.compose.Composable

            $source
        """.trimIndent(),
        expectedTransformed,
        "",
        dumpTree
    )

    @Test
    fun testCallingProperties(): Unit = composerParam(
        """
            @Composable val bar: Int get() { return 123 }

            @Composable fun Example() {
                bar
            }
        """,
        """
            val bar: Int
              get() {
                return 123
              }
            @Composable
            fun Example(%composer: Composer<*>?) {
              bar
            }
        """
    )

    @Test
    fun testAbstractComposable(): Unit = composerParam(
        """
            abstract class BaseFoo {
                @Composable
                abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @Composable
                override fun bar() {}
            }
        """,
        """
            abstract class BaseFoo {
              @Composable
              abstract fun bar(%composer: Composer<*>?)
            }
            class FooImpl : BaseFoo {
              @Composable
              override fun bar(%composer: Composer<*>?) { }
            }
        """
    )

    @Test
    fun testLocalClassAndObjectLiterals(): Unit = composerParam(
        """
            @Composable
            fun Wat() {}

            @Composable
            fun Foo(x: Int) {
                Wat()
                @Composable fun goo() { Wat() }
                class Bar {
                    @Composable fun baz() { Wat() }
                }
                goo()
                Bar().baz()
            }
        """,
        """
            @Composable
            fun Wat(%composer: Composer<*>?) { }
            @Composable
            fun Foo(x: Int, %composer: Composer<*>?) {
              Wat(%composer)
              @Composable
              fun goo(%composer: Composer<*>?) {
                Wat(%composer)
              }
              class Bar {
                @Composable
                fun baz(%composer: Composer<*>?) {
                  Wat(%composer)
                }
              }
              goo(%composer)
              Bar().baz(%composer)
            }
        """
    )

    @Test
    fun testNonComposableCode(): Unit = composerParam(
        """
            fun A() {}
            val b: Int get() = 123
            fun C(x: Int) {
                var x = 0
                x++

                class D {
                    fun E() { A() }
                    val F: Int get() = 123
                }
                val g = object { fun H() {} }
            }
            fun I(block: () -> Unit) { block() }
            fun J() {
                I {
                    I {
                        A()
                    }
                }
            }
        """,
        """
            fun A() { }
            val b: Int
              get() {
                return 123
              }
            fun C(x: Int) {
              var x = 0
              x++
              class D {
                fun E() {
                  A()
                }
                val F: Int
                  get() {
                    return 123
                  }
              }
              val g = object {
                fun H() { }
              }
            }
            fun I(block: Function0<Unit>) {
              block()
            }
            fun J() {
              I {
                I {
                  A()
                }
              }
            }
        """
    )

    @Test
    fun testCircularCall(): Unit = composerParam(
        """
            @Composable fun Example() {
                Example()
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer<*>?) {
              Example(%composer)
            }
        """
    )

    @Test
    fun testInlineCall(): Unit = composerParam(
        """
            @Composable inline fun Example(children: @Composable() () -> Unit) {
                children()
            }

            @Composable fun Test() {
                Example {}
            }
        """,
        """
            @Composable
            fun Example(children: Function1<Composer<*>, Unit>, %composer: Composer<*>?) {
              children(%composer)
            }
            @Composable
            fun Test(%composer: Composer<*>?) {
              Example({ %composer: Composer<*>? ->
              }, %composer)
            }
        """
    )

    @Test
    fun testExtensionSetterEmit(): Unit = composerParam(
        """
            import android.widget.TextView

            private fun TextView.setRef(ref: (TextView) -> Unit) {}

            @Composable
            fun Test() {
                TextView(ref = {  })
            }
        """,
        """
            private fun TextView.setRef(ref: Function1<TextView, Unit>) { }
            @Composable
            fun Test(%composer: Composer<*>?) {
              TextView(
                ref = { it: TextView ->
                }
              )
            }
        """
    )

    @Test
    fun testDexNaming(): Unit = composerParam(
        """
            @Composable
            val myProperty: () -> Unit get() {
                return {  }
            }
        """,
        """
            val myProperty: Function0<Unit>
              get() {
                return {
                }
              }
        """
    )

    @Test
    fun testInnerClass(): Unit = composerParam(
        """
            interface A {
                fun b() {}
            }
            class C {
                val foo = 1
                inner class D : A {
                    override fun b() {
                        print(foo)
                    }
                }
            }
        """,
        """
            interface A {
              open fun b() { }
            }
            class C {
              val foo: Int = 1
              inner class D : A {
                override fun b() {
                  print(foo)
                }
              }
            }
        """
    )
}