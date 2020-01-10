/*
 * Copyright 2019 The Android Open Source Project
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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposerParamSignatureTests : AbstractCodegenSignatureTest() {

    @Test
    fun testCorrectComposerPassed1(): Unit = checkComposerParam(
        """
            val a = makeComposer()
            fun run() {
                invokeComposable(a) {
                    assertComposer(a)
                }
            }
        """
    )

    @Test
    fun testCorrectComposerPassed2(): Unit = checkComposerParam(
        """
            val a = makeComposer()
            @Composable fun Foo() {
                assertComposer(a)
            }
            fun run() {
                invokeComposable(a) {
                    Foo()
                }
            }
        """
    )

    @Test
    fun testCorrectComposerPassed3(): Unit = checkComposerParam(
        """
            val a = makeComposer()
            val b = makeComposer()
            @Composable fun Callback(fn: () -> Unit) {
                fn()
            }
            fun run() {
                invokeComposable(a) {
                    assertComposer(a)
                    Callback {
                        invokeComposable(b) {
                            assertComposer(b)
                        }
                    }
                }
            }
        """
    )

    @Test
    fun testCorrectComposerPassed4(): Unit = checkComposerParam(
        """
            val a = makeComposer()
            val b = makeComposer()
            @Composable fun makeInt(): Int {
                assertComposer(a)
                return 10
            }
            @Composable fun WithDefault(x: Int = makeInt()) {}
            fun run() {
                invokeComposable(a) {
                    assertComposer(a)
                    WithDefault()
                    WithDefault(10)
                }
                invokeComposable(b) {
                    assertComposer(b)
                    WithDefault(10)
                }
            }
        """
    )

    @Test
    fun testCorrectComposerPassed5(): Unit = checkComposerParam(
        """
            val a = makeComposer()
            @Composable fun Wrap(children: @Composable() () -> Unit) {
                children()
            }
            fun run() {
                invokeComposable(a) {
                    assertComposer(a)
                    Wrap {
                        assertComposer(a)
                        Wrap {
                            assertComposer(a)
                            Wrap {
                                assertComposer(a)
                            }
                        }
                    }
                }
            }
        """
    )

    @Test
    fun testDefaultParameters(): Unit = checkApi(
        """
            @Composable fun Foo(x: Int = 0) {

            }
        """,
        """
            public final class TestKt {
              public final static Foo(ILandroidx/compose/Composer;)V
              public static synthetic Foo%default(ILandroidx/compose/Composer;ILjava/lang/Object;)V
            }
        """
    )

    @Test
    fun testDefaultExpressionsWithComposableCall(): Unit = checkApi(
        """
            @Composable fun <T> identity(value: T): T = value
            @Composable fun Foo(x: Int = identity(20)) {

            }
            @Composable fun test() {
                Foo()
                Foo(10)
            }
        """,
        """
            public final class TestKt {
              public final static identity(Ljava/lang/Object;Landroidx/compose/Composer;)Ljava/lang/Object;
              public final static Foo(ILandroidx/compose/Composer;)V
              public static synthetic Foo%default(ILandroidx/compose/Composer;ILjava/lang/Object;)V
              public final static test(Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testBasicCallAndParameterUsage(): Unit = checkApi(
        """
            @Composable fun Foo(a: Int, b: String) {
                print(a)
                print(b)
                Bar(a, b)
            }

            @Composable fun Bar(a: Int, b: String) {
                print(a)
                print(b)
            }
        """,
        """
            public final class TestKt {
              public final static Foo(ILjava/lang/String;Landroidx/compose/Composer;)V
              public final static Bar(ILjava/lang/String;Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testCallFromInlinedLambda(): Unit = checkApi(
        """
            @Composable fun Foo() {
                listOf(1, 2, 3).forEach { Bar(it) }
            }

            @Composable fun Bar(a: Int) {}
        """,
        """
            public final class TestKt {
              public final static Foo(Landroidx/compose/Composer;)V
              public final static Bar(ILandroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testBasicLambda(): Unit = checkApi(
        """
            val foo = @Composable { x: Int -> print(x)  }
            @Composable fun Bar() {
              foo(123)
            }
        """,
        """
            public final class TestKt {
              private final static Lkotlin/jvm/functions/Function2; foo
              public final static getFoo()Lkotlin/jvm/functions/Function2;
              public final static Bar(Landroidx/compose/Composer;)V
              public final static <clinit>()V
              final static INNERCLASS TestKt%foo%1 null null
            }
            final class TestKt%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>()V
              public final invoke(ILandroidx/compose/Composer;)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%foo%1 null null
              OUTERCLASS TestKt <clinit> ()V
            }
        """
    )

    @Test
    fun testLocalLambda(): Unit = checkApi(
        """
            @Composable fun Bar(children: @Composable() () -> Unit) {
                val foo = @Composable { x: Int -> print(x)  }
                foo(123)
                children()
            }
        """,
        """
            public final class TestKt {
              public final static Bar(Lkotlin/jvm/functions/Function1;Landroidx/compose/Composer;)V
              final static INNERCLASS TestKt%Bar%foo%1 null null
            }
            final class TestKt%Bar%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>()V
              public final invoke(ILandroidx/compose/Composer;)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%foo%1 null null
              OUTERCLASS TestKt Bar (Lkotlin/jvm/functions/Function1;Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testNesting(): Unit = checkApi(
        """
            @Composable fun Wrap(children: @Composable() (x: Int) -> Unit) {
                children(123)
            }
            @Composable fun App(x: Int) {
                print(x)
                Wrap { a ->
                    print(a)
                    print(x)
                    Wrap { b ->
                        print(x)
                        print(a)
                        print(b)
                    }
                }
            }
        """,
        """
            public final class TestKt {
              public final static Wrap(Lkotlin/jvm/functions/Function2;Landroidx/compose/Composer;)V
              public final static App(ILandroidx/compose/Composer;)V
              final static INNERCLASS TestKt%App%1 null null
            }
            final class TestKt%App%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>(I)V
              public final invoke(ILandroidx/compose/Composer;)V
              private final synthetic I %x
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt App (ILandroidx/compose/Composer;)V
            }
            final class TestKt%App%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>(II)V
              public final invoke(ILandroidx/compose/Composer;)V
              private final synthetic I %x
              private final synthetic I %a
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt%App%1 invoke (ILandroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testComposableInterface(): Unit = checkApi(
        """
            interface Foo {
                @Composable fun bar()
            }

            class FooImpl : Foo {
                @Composable override fun bar() {}
            }
        """,
        """
            public abstract interface Foo {
              public abstract bar(Landroidx/compose/Composer;)V
            }
            public final class FooImpl implements Foo {
              public <init>()V
              public bar(Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testAbstractComposable(): Unit = checkApi(
        """
            abstract class BaseFoo {
                @Composable abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @Composable override fun bar() {}
            }
        """,
        """
            public abstract class BaseFoo {
              public <init>()V
              public abstract bar(Landroidx/compose/Composer;)V
            }
            public final class FooImpl extends BaseFoo {
              public <init>()V
              public bar(Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testLocalClassAndObjectLiterals(): Unit = checkApi(
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
            public final class TestKt {
              public final static Wat(Landroidx/compose/Composer;)V
              public final static Foo(ILandroidx/compose/Composer;)V
              private final static Foo%goo(Landroidx/compose/Composer;)V
              public final static INNERCLASS TestKt%Foo%Bar null Bar
            }
            public final class TestKt%Foo%Bar {
              <init>()V
              public final baz(Landroidx/compose/Composer;)V
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testNonComposableCode(): Unit = checkApi(
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
            public final class TestKt {
              public final static A()V
              public final static getB()I
              public final static C(I)V
              public final static I(Lkotlin/jvm/functions/Function0;)V
              public final static J()V
              public final static INNERCLASS TestKt%C%D null D
              public final static INNERCLASS TestKt%C%g%1 null null
              final static INNERCLASS TestKt%J%1 null null
            }
            public final class TestKt%C%D {
              <init>()V
              public final E()V
              public final getF()I
              public final static INNERCLASS TestKt%C%D null D
              OUTERCLASS TestKt C (I)V
            }
            public final class TestKt%C%g%1 {
              <init>()V
              public final H()V
              public final static INNERCLASS TestKt%C%g%1 null null
              OUTERCLASS TestKt C (I)V
            }
            final class TestKt%J%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%J%1%1 null null
              final static INNERCLASS TestKt%J%1 null null
              OUTERCLASS TestKt J ()V
            }
            final class TestKt%J%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%J%1%1 null null
              final static INNERCLASS TestKt%J%1 null null
              OUTERCLASS TestKt%J%1 invoke ()V
            }
        """
    )

    @Test
    fun testCircularCall(): Unit = checkApi(
        """
            @Composable fun Example() {
                Example()
            }
        """,
        """
            public final class TestKt {
              public final static Example(Landroidx/compose/Composer;)V
            }
        """
    )

    override fun setUp() {
        ComposeFlags.COMPOSER_PARAM = true
        super.setUp()
    }

    override fun tearDown() {
        super.tearDown()
        ComposeFlags.COMPOSER_PARAM = false
    }
}