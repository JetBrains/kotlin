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
    private fun composerParam(
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.Composable
            import androidx.compose.Direct

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

            @Direct @Composable fun Example() {
                bar
            }
        """,
        """
            val bar: Int
              get() {
                %composer.startReplaceableGroup(%key)
                val tmp0 = 123
                %composer.endReplaceableGroup()
                return tmp0
              }
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              bar
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testAbstractComposable(): Unit = composerParam(
        """
            abstract class BaseFoo {
                @Direct
                @Composable
                abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @Direct
                @Composable
                override fun bar() {}
            }
        """,
        """
            abstract class BaseFoo {
              @Direct
              @Composable
              abstract fun bar(%composer: Composer<*>?, %key: Int, %changed: Int)
            }
            class FooImpl : BaseFoo {
              @Direct
              @Composable
              override fun bar(%composer: Composer<*>?, %key: Int, %changed: Int) {
                %composer.startReplaceableGroup(%key)
                %composer.endReplaceableGroup()
              }
            }
        """
    )

    @Test
    fun testLocalClassAndObjectLiterals(): Unit = composerParam(
        """
            @Direct
            @Composable
            fun Wat() {}

            @Direct
            @Composable
            fun Foo(x: Int) {
                Wat()
                @Direct
                @Composable fun goo() { Wat() }
                class Bar {
                    @Direct
                    @Composable fun baz() { Wat() }
                }
                goo()
                Bar().baz()
            }
        """,
        """
            @Direct
            @Composable
            fun Wat(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.endReplaceableGroup()
            }
            @Direct
            @Composable
            fun Foo(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              Wat(%composer, <>, 0)
              @Direct
              @Composable
              fun goo(%composer: Composer<*>?, %key: Int, %changed: Int) {
                %composer.startReplaceableGroup(%key)
                Wat(%composer, <>, 0)
                %composer.endReplaceableGroup()
              }
              class Bar {
                @Direct
                @Composable
                fun baz(%composer: Composer<*>?, %key: Int, %changed: Int) {
                  %composer.startReplaceableGroup(%key)
                  Wat(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
              }
              goo(%composer, <>, 0)
              Bar().baz(%composer, <>, 0)
              %composer.endReplaceableGroup()
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
            @Direct
            @Composable fun Example() {
                Example()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              Example(%composer, <>, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testInlineCall(): Unit = composerParam(
        """
            @Composable inline fun Example(children: @Composable () -> Unit) {
                children()
            }

            @Direct
            @Composable fun Test() {
                Example {}
            }
        """,
        """
            @Composable
            fun Example(children: Function3<Composer<*>, Int, Int, Unit>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              children(%composer, <>, 0b0110 and %changed)
              %composer.endReplaceableGroup()
            }
            @Direct
            @Composable
            fun Test(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              Example({ %composer: Composer<*>?, %key: Int, %changed: Int ->
                if (%changed and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }, %composer, <>, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testExtensionSetterEmit(): Unit = composerParam(
        """
            import android.widget.TextView

            private fun TextView.setRef(ref: (TextView) -> Unit) {}

            @Direct
            @Composable
            fun Test() {
                TextView(ref = {  })
            }
        """,
        """
            private fun TextView.setRef(ref: Function1<TextView, Unit>) { }
            @Direct
            @Composable
            fun Test(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0 = remember({
                { it: TextView ->
                }
              }, %composer, <>, 0)
              %composer.emit(-1248659145, { context: @[ParameterName(name = 'context')] Context ->
                TextView(context)
              }
              ) {
                set(tmp0) { p0: Function1<TextView, Unit> ->
                  setRef(p0)
                }
              }
              %composer.endReplaceableGroup()
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
                %composer.startReplaceableGroup(%key)
                val tmp0 = {
                }
                %composer.endReplaceableGroup()
                return tmp0
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