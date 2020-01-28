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
    fun testLambdaReorderedParameter(): Unit = checkApi(
        """
            @Composable fun Foo(a: String, b: () -> Unit) { }
            @Composable fun Example() {
                Foo(b={}, a="Hello, world!")
            }
        """,
        """
            public final class TestKt {
              public final static Foo(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/Composer;)V
              public final static Example(Landroidx/compose/Composer;)V
              public final static synthetic Foo(Ljava/lang/String;Lkotlin/jvm/functions/Function0;)V
              public final static synthetic Example()V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%Example%1 null null
              final static INNERCLASS TestKt%Example%4 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Ljava/lang/String; %a
              private final synthetic Lkotlin/jvm/functions/Function0; %b
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/Composer;)V
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%1 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;)V
            }
            final class TestKt%Example%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%4 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testRemappedTypes(): Unit = checkApi(
        """
            class A {
                fun makeA(): A { return A() }
                fun makeB(): B { return B() }
                class B() {
                }
            }
            class C {
                fun useAB() {
                    val a = A()
                    a.makeA()
                    a.makeB()
                    val b = A.B()
                }
            }
        """,
        """
            public final class A {
              public <init>()V
              public final makeA()LA;
              public final makeB()LA%B;
              public final static INNERCLASS A%B A B
            }
            public final class A%B {
              public <init>()V
              public final static INNERCLASS A%B A B
            }
            public final class C {
              public <init>()V
              public final useAB()V
            }
        """
    )

    @Test
    fun testCorrectComposerPassed1(): Unit = checkComposerParam(
        """
            var a: Composer<*>? = null
            fun run() {
                a = makeComposer()
                invokeComposable(a) {
                    assertComposer(a)
                }
            }
        """
    )

    @Test
    fun testSimpleEmits(): Unit = checkApi(
        """
            import android.widget.LinearLayout
            import android.widget.TextView

            @Composable fun Example() {
                LinearLayout(id=123) {
                    TextView(text="Hello World")
                }
            }
        """,
        """
            public final class TestKt {
              public final static Example(Landroidx/compose/Composer;)V
              public final static synthetic Example()V
              final static INNERCLASS TestKt%Example%4 null null
            }
            final class TestKt%Example%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%4 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testCorrectComposerPassed2(): Unit = checkComposerParam(
        """
            var a: Composer<*>? = null
            @Composable fun Foo() {
                assertComposer(a)
            }
            fun run() {
                a = makeComposer()
                invokeComposable(a) {
                    Foo()
                }
            }
        """
    )

    @Test
    fun testCorrectComposerPassed3(): Unit = checkComposerParam(
        """
            var a: Composer<*>? = null
            var b: Composer<*>? = null
            @Composable fun Callback(fn: () -> Unit) {
                fn()
            }
            fun run() {
                a = makeComposer()
                b = makeComposer()
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
            var a: Composer<*>? = null
            var b: Composer<*>? = null
            @Composable fun makeInt(): Int {
                assertComposer(a)
                return 10
            }
            @Composable fun WithDefault(x: Int = makeInt()) {}
            fun run() {
                a = makeComposer()
                b = makeComposer()
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
            var a: Composer<*>? = null
            @Composable fun Wrap(children: @Composable() () -> Unit) {
                children()
            }
            fun run() {
                a = makeComposer()
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
    fun testCorrectComposerPassed6(): Unit = checkComposerParam(
        """
            import android.widget.TextView

            fun run() {
                invokeComposable(makeComposer()) {
                    LinearLayout(id=123) {
                        TextView(text="Hello World")
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
              public final static synthetic Foo(I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILandroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %x
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;)V
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
              public final static synthetic identity(Ljava/lang/Object;)Ljava/lang/Object;
              public final static synthetic Foo(I)V
              public final static synthetic test()V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%test%5 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILandroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %x
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;)V
            }
            final class TestKt%test%5 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%test%5 null null
              OUTERCLASS TestKt test (Landroidx/compose/Composer;)V
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
              public final static synthetic Foo(ILjava/lang/String;)V
              public final static synthetic Bar(ILjava/lang/String;)V
              final static INNERCLASS TestKt%Foo%3 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Foo%3 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILjava/lang/String;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %a
              private final synthetic Ljava/lang/String; %b
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%3 null null
              OUTERCLASS TestKt Foo (ILjava/lang/String;Landroidx/compose/Composer;)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILjava/lang/String;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %a
              private final synthetic Ljava/lang/String; %b
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (ILjava/lang/String;Landroidx/compose/Composer;)V
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
              public final static synthetic Foo()V
              public final static synthetic Bar(I)V
              final static INNERCLASS TestKt%Foo%2 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Foo%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%2 null null
              OUTERCLASS TestKt Foo (Landroidx/compose/Composer;)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILandroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %a
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (ILandroidx/compose/Composer;)V
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
              public final static synthetic Bar()V
              public final static <clinit>()V
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%foo%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (Landroidx/compose/Composer;)V
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
              public final static synthetic Bar(Lkotlin/jvm/functions/Function0;)V
              final static INNERCLASS TestKt%Bar%foo%1 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Bar%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>()V
              public final invoke(ILandroidx/compose/Composer;)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%foo%1 null null
              OUTERCLASS TestKt Bar (Lkotlin/jvm/functions/Function1;Landroidx/compose/Composer;)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Lkotlin/jvm/functions/Function1;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Lkotlin/jvm/functions/Function1; %children
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
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
              public final static synthetic Wrap(Lkotlin/jvm/functions/Function1;)V
              public final static synthetic App(I)V
              final static INNERCLASS TestKt%Wrap%1 null null
              final static INNERCLASS TestKt%App%1 null null
              final static INNERCLASS TestKt%App%4 null null
            }
            final class TestKt%Wrap%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Lkotlin/jvm/functions/Function2;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Lkotlin/jvm/functions/Function2; %children
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Wrap%1 null null
              OUTERCLASS TestKt Wrap (Lkotlin/jvm/functions/Function2;Landroidx/compose/Composer;)V
            }
            final class TestKt%App%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>(I)V
              public final invoke(ILandroidx/compose/Composer;)V
              private final synthetic I %x
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%invoke%1 null null
              final static INNERCLASS TestKt%App%1%invoke%4 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt App (ILandroidx/compose/Composer;)V
            }
            final class TestKt%App%1%invoke%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              synthetic <init>(II)V
              public final invoke(ILandroidx/compose/Composer;)V
              private final synthetic I %x
              private final synthetic I %a
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%invoke%1%invoke%1 null null
              final static INNERCLASS TestKt%App%1%invoke%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt%App%1 invoke (ILandroidx/compose/Composer;)V
            }
            final class TestKt%App%1%invoke%1%invoke%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(LTestKt%App%1%invoke%1;ILandroidx/compose/Composer;II)V
              public final invoke()V
              private final synthetic LTestKt%App%1%invoke%1; %tmp1_rcvr
              private final synthetic I %b
              private final synthetic Landroidx/compose/Composer; %%composer
              private final synthetic I %x
              private final synthetic I %a
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%invoke%1%invoke%1 null null
              final static INNERCLASS TestKt%App%1%invoke%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt%App%1%invoke%1 invoke (ILandroidx/compose/Composer;)V
            }
            final class TestKt%App%1%invoke%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(LTestKt%App%1;ILandroidx/compose/Composer;I)V
              public final invoke()V
              private final synthetic LTestKt%App%1; %tmp1_rcvr
              private final synthetic I %a
              private final synthetic Landroidx/compose/Composer; %%composer
              private final synthetic I %x
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%invoke%4 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt%App%1 invoke (ILandroidx/compose/Composer;)V
            }
            final class TestKt%App%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILandroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %x
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%App%4 null null
              OUTERCLASS TestKt App (ILandroidx/compose/Composer;)V
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
              public abstract synthetic bar()V
            }
            public final class FooImpl implements Foo {
              public <init>()V
              public bar(Landroidx/compose/Composer;)V
              public synthetic bar()V
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(LFooImpl;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic LFooImpl; %tmp1_rcvr
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS FooImpl%bar%1 null null
              OUTERCLASS FooImpl bar (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testComposableTopLevelProperty(): Unit = checkApi(
        """
            @Composable val foo: Int get() { return 123 }
        """,
        """
            public final class TestKt {
              public final static synthetic getFoo()I
              public static synthetic foo%annotations()V
              public final static getFoo(Landroidx/compose/Composer;)I
            }
        """
    )
    @Test
    fun testComposableProperty(): Unit = checkApi(
        """
            class Foo {
                @Composable val foo: Int get() { return 123 }
            }
        """,
        """
            public final class Foo {
              public <init>()V
              public final synthetic getFoo()I
              public static synthetic foo%annotations()V
              public final getFoo(Landroidx/compose/Composer;)I
            }
        """
    )
    @Test
    fun testCallingProperties(): Unit = checkApi(
        """
            @Composable val bar: Int get() { return 123 }

            @Composable fun Example() {
                bar
            }
        """,
        """
            public final class TestKt {
              public final static synthetic getBar()I
              public static synthetic bar%annotations()V
              public final static Example(Landroidx/compose/Composer;)V
              public final static synthetic Example()V
              public final static getBar(Landroidx/compose/Composer;)I
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%1 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;)V
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
              public abstract synthetic bar()V
            }
            public final class FooImpl extends BaseFoo {
              public <init>()V
              public bar(Landroidx/compose/Composer;)V
              public synthetic bar()V
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(LFooImpl;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic LFooImpl; %tmp1_rcvr
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS FooImpl%bar%1 null null
              OUTERCLASS FooImpl bar (Landroidx/compose/Composer;)V
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
              public final static synthetic Wat()V
              public final static synthetic Foo(I)V
              private final static Foo%goo(Landroidx/compose/Composer;)V
              public final static synthetic access%Foo%goo%0(Landroidx/compose/Composer;)V
              final static INNERCLASS TestKt%Wat%1 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              final static INNERCLASS TestKt%Foo%8 null null
              final static INNERCLASS TestKt%Foo%3%3 null null
            }
            final class TestKt%Wat%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Wat%1 null null
              OUTERCLASS TestKt Wat (Landroidx/compose/Composer;)V
            }
            public final class TestKt%Foo%Bar {
              <init>()V
              public final baz(Landroidx/compose/Composer;)V
              public final synthetic baz()V
              final static INNERCLASS TestKt%Foo%Bar%baz%3 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;)V
            }
            final class TestKt%Foo%Bar%baz%3 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(LTestKt%Foo%Bar;Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic LTestKt%Foo%Bar; %tmp1_rcvr
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%Bar%baz%3 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              OUTERCLASS TestKt%Foo%Bar baz (Landroidx/compose/Composer;)V
            }
            final class TestKt%Foo%8 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(ILandroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic I %x
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%8 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;)V
            }
            final class TestKt%Foo%3%3 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%3%3 null null
              OUTERCLASS TestKt Foo%goo (Landroidx/compose/Composer;)V
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
              public final static synthetic Example()V
              final static INNERCLASS TestKt%Example%3 null null
            }
            final class TestKt%Example%3 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%3 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testInlineCall(): Unit = checkApi(
        """
            @Composable inline fun Example(children: @Composable() () -> Unit) {
                children()
            }

            @Composable fun Test() {
                Example {}
            }
        """,
        """
            public final class TestKt {
              public final static Example(Lkotlin/jvm/functions/Function1;Landroidx/compose/Composer;)V
              public final static Test(Landroidx/compose/Composer;)V
              public final static synthetic Example(Lkotlin/jvm/functions/Function0;)V
              public final static synthetic Test()V
              final static INNERCLASS TestKt%Test%2 null null
            }
            final class TestKt%Test%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%2 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testExtensionSetterEmit(): Unit = checkApi(
        """
            import android.widget.TextView

            private fun TextView.setRef(ref: (TextView) -> Unit) {}

            @Composable
            fun Test() {
                TextView(ref = {  })
            }
        """,
        """
            public final class TestKt {
              private final static setRef(Landroid/widget/TextView;Lkotlin/jvm/functions/Function1;)V
              public final static Test(Landroidx/compose/Composer;)V
              public final static synthetic Test()V
              final static INNERCLASS TestKt%Test%1 null null
              final static INNERCLASS TestKt%Test%4 null null
            }
            final class TestKt%Test%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function1 {
              synthetic <init>()V
              public final invoke(Landroid/widget/TextView;)V
              public synthetic bridge invoke(Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%1 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;)V
            }
            final class TestKt%Test%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              synthetic <init>(Landroidx/compose/Composer;)V
              public final invoke()V
              private final synthetic Landroidx/compose/Composer; %%composer
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%4 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;)V
            }
        """
    )

    @Test
    fun testInnerClass(): Unit = checkApi(
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
            public abstract interface A {
              public abstract b()V
              public final static INNERCLASS A%DefaultImpls A DefaultImpls
            }
            public final class A%DefaultImpls {
              public static b(LA;)V
              public final static INNERCLASS A%DefaultImpls A DefaultImpls
            }
            public final class C {
              public <init>()V
              private final I foo
              public final getFoo()I
              public final INNERCLASS C%D C D
            }
            public final class C%D implements A {
              public <init>(LC;)V
              public b()V
              final synthetic LC; this%0
              public final INNERCLASS C%D C D
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