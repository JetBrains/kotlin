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

/* ktlint-disable max-line-length */
@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposerParamSignatureTests : AbstractCodegenSignatureTest() {

    @Test
    fun testParameterlessChildrenLambdasReused(): Unit = checkApi(
        """
            @Composable fun Foo(children: @Composable () -> Unit) {
            }
            @Composable fun Bar() {
                Foo {}
            }
        """,
        // We expect 3 lambda classes. One for Foo's restart group. One for Bar's restart group.
        // and one for the children lambda passed into Foo. Importantly, there is no lambda for
        // the children lambda's restart group because we are using the lambda itself.
        """
            public final class TestKt {
              public final static Foo(Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
              public final static Bar(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%Bar%2 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(Lkotlin/jvm/functions/Function3;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic Lkotlin/jvm/functions/Function3; %children
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>()V
              public final invoke(Landroidx/compose/Composer;II)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (Landroidx/compose/Composer;II)V
            }
            final class TestKt%Bar%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%2 null null
              OUTERCLASS TestKt Bar (Landroidx/compose/Composer;II)V
            }
        """
    )

    @Test
    fun testNoComposerNullCheck(): Unit = validateBytecode(
        """
        @Composable fun Foo() {}
        """
    ) {
        assert(!it.contains("INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull"))
    }

    @Test
    fun testPrimitiveChangedCalls(): Unit = validateBytecode(
        """
        @Composable fun Foo(
            a: Boolean,
            b: Char,
            c: Byte,
            d: Short,
            e: Int,
            f: Float,
            g: Long,
            h: Double
        ) {}
        """
    ) {
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (Z)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (C)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (B)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (S)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (I)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (F)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (J)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (D)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (Ljava/lang/Object;)Z"))
    }

    @Test
    fun testNonPrimitiveChangedCalls(): Unit = validateBytecode(
        """
        import androidx.compose.Stable

        @Stable class Bar
        @Composable fun Foo(a: Bar) {}
        """
    ) {
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (Z)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (C)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (B)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (S)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (I)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (F)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (J)Z"))
        assert(!it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (D)Z"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (Ljava/lang/Object;)Z"))
    }

    @Test
    fun testInlineClassChangedCalls(): Unit = validateBytecode(
        """
        inline class Bar(val value: Int)
        @Composable fun Foo(a: Bar) {}
        """
    ) {
        assert(!it.contains("INVOKESTATIC Bar.box-impl (I)LBar;"))
        assert(it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (I)Z"))
        assert(
            !it.contains("INVOKEVIRTUAL androidx/compose/Composer.changed (Ljava/lang/Object;)Z")
        )
    }

    @Test
    fun testNullableInlineClassChangedCalls(): Unit = validateBytecode(
        """
        inline class Bar(val value: Int)
        @Composable fun Foo(a: Bar?) {}
        """
    ) {
        val testClass = it.split("public final class ").single { it.startsWith("test/TestKt") }
        assert(!testClass.contains(
            "INVOKEVIRTUAL Bar.unbox-impl ()I"
        ))
        assert(!testClass.contains(
            "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;"
        ))
        assert(testClass.contains(
            "INVOKEVIRTUAL androidx/compose/Composer.changed (Ljava/lang/Object;)Z"
        ))
    }

    @Test
    fun testNoNullCheckForPassedParameters(): Unit = validateBytecode(
        """
        inline class Bar(val value: Int)
        fun nonNull(bar: Bar) {}
        @ComposableContract(restartable = false) @Composable fun Foo(bar: Bar = Bar(123)) {
            nonNull(bar)
        }
        """
    ) {
        assert(it.contains("public final static Foo-RDdOdTo(ILandroidx/compose/Composer;III)V"))
    }

    @Test
    fun testNoComposerNullCheck2(): Unit = validateBytecode(
        """
        val foo = @Composable {}
        val bar = @Composable { x: Int -> }
        """
    ) {
        assert(!it.contains("INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull"))
    }

    @Test
    fun testComposableLambdaInvoke(): Unit = validateBytecode(
        """
        @Composable fun NonNull(content: @Composable() () -> Unit) {
            content.invoke()
        }
        @Composable fun Nullable(content: (@Composable() () -> Unit)?) {
            content?.invoke()
        }
        """
    ) {
        assert(!it.contains(
            "INVOKEINTERFACE kotlin/jvm/functions/Function0.invoke ()Ljava/lang/Object; (itf)"
        ))
    }

    @Test
    fun testAnonymousParamNaming(): Unit = validateBytecode(
        """
        @Composable
        fun Foo(children: @Composable (a: Int, b: Int) -> Unit) {}
        @Composable
        fun test() {
            Foo { _, _ -> }
        }
        """
    ) {
        assert(!it.contains("%anonymous parameter 0%"))
    }

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
              public final static Foo(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/Composer;II)V
              public final static Example(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%Example%1 null null
              final static INNERCLASS TestKt%Example%2 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(Ljava/lang/String;Lkotlin/jvm/functions/Function0;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic Ljava/lang/String; %a
              private final synthetic Lkotlin/jvm/functions/Function0; %b
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/Composer;II)V
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%1 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;II)V
            }
            final class TestKt%Example%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%2 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;II)V
            }
            """
    )

    @Test
    fun testAmbientCurrent(): Unit = checkApi(
        """
            val a = ambientOf { 123 }
            @Composable fun Foo() {
                val b = a.current
                print(b)
            }
        """,
        """
            public final class TestKt {
              private final static Landroidx/compose/ProvidableAmbient; a
              public final static getA()Landroidx/compose/ProvidableAmbient;
              public final static Foo(Landroidx/compose/Composer;II)V
              public final static <clinit>()V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%a%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (Landroidx/compose/Composer;II)V
            }
            final class TestKt%a%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()I
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%a%1 null null
              OUTERCLASS TestKt <clinit> ()V
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
    fun testDataClassHashCode(): Unit = validateBytecode(
        """
        data class Foo(
            val bar: @Composable () -> Unit
        )
        """) {
        assert(!it.contains("CHECKCAST kotlin/jvm/functions/Function0"))
    }

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
              public final static Example(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Example%4 null null
            }
            final class TestKt%Example%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%4 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;II)V
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
            @Composable fun Wrap(children: @Composable () -> Unit) {
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
              public final static Foo(ILandroidx/compose/Composer;III)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(III)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %x
              private final synthetic I %%changed
              private final synthetic I %%default
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;III)V
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
              public final static identity(Ljava/lang/Object;Landroidx/compose/Composer;II)Ljava/lang/Object;
              public final static Foo(ILandroidx/compose/Composer;III)V
              public final static test(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%test%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(III)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %x
              private final synthetic I %%changed
              private final synthetic I %%default
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;III)V
            }
            final class TestKt%test%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%test%1 null null
              OUTERCLASS TestKt test (Landroidx/compose/Composer;II)V
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
              public final static Foo(ILjava/lang/String;Landroidx/compose/Composer;II)V
              public final static Bar(ILjava/lang/String;Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(ILjava/lang/String;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %a
              private final synthetic Ljava/lang/String; %b
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILjava/lang/String;Landroidx/compose/Composer;II)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(ILjava/lang/String;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %a
              private final synthetic Ljava/lang/String; %b
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (ILjava/lang/String;Landroidx/compose/Composer;II)V
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
              public final static Foo(Landroidx/compose/Composer;II)V
              public final static Bar(ILandroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%2 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Foo%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%2 null null
              OUTERCLASS TestKt Foo (Landroidx/compose/Composer;II)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(II)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %a
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (ILandroidx/compose/Composer;II)V
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
              private final static Lkotlin/jvm/functions/Function4; foo
              public final static getFoo()Lkotlin/jvm/functions/Function4;
              public final static Bar(Landroidx/compose/Composer;II)V
              public final static <clinit>()V
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%foo%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (Landroidx/compose/Composer;II)V
            }
            final class TestKt%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function4 {
              <init>()V
              public final invoke(ILandroidx/compose/Composer;II)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%foo%1 null null
              OUTERCLASS TestKt <clinit> ()V
            }
        """
    )

    @Test
    fun testLocalLambda(): Unit = checkApi(
        """
            @Composable fun Bar(children: @Composable () -> Unit) {
                val foo = @Composable { x: Int -> print(x)  }
                foo(123)
                children()
            }
        """,
        """
            public final class TestKt {
              public final static Bar(Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Bar%foo%1 null null
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Bar%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function4 {
              <init>()V
              public final invoke(ILandroidx/compose/Composer;II)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%foo%1 null null
              OUTERCLASS TestKt Bar (Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(Lkotlin/jvm/functions/Function3;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic Lkotlin/jvm/functions/Function3; %children
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Bar%1 null null
              OUTERCLASS TestKt Bar (Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
            }
        """
    )

    @Test
    fun testNesting(): Unit = checkApi(
        """
            @Composable fun Wrap(children: @Composable (x: Int) -> Unit) {
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
              public final static Wrap(Lkotlin/jvm/functions/Function4;Landroidx/compose/Composer;II)V
              public final static App(ILandroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Wrap%1 null null
              final static INNERCLASS TestKt%App%1 null null
              final static INNERCLASS TestKt%App%2 null null
            }
            final class TestKt%Wrap%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(Lkotlin/jvm/functions/Function4;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic Lkotlin/jvm/functions/Function4; %children
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Wrap%1 null null
              OUTERCLASS TestKt Wrap (Lkotlin/jvm/functions/Function4;Landroidx/compose/Composer;II)V
            }
            final class TestKt%App%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function4 {
              <init>(I)V
              public final invoke(ILandroidx/compose/Composer;II)V
              private final synthetic I %x
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt App (ILandroidx/compose/Composer;II)V
            }
            final class TestKt%App%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function4 {
              <init>(II)V
              public final invoke(ILandroidx/compose/Composer;II)V
              private final synthetic I %x
              private final synthetic I %a
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%1%1 null null
              final static INNERCLASS TestKt%App%1 null null
              OUTERCLASS TestKt%App%1 invoke (ILandroidx/compose/Composer;II)V
            }
            final class TestKt%App%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(II)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %x
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%App%2 null null
              OUTERCLASS TestKt App (ILandroidx/compose/Composer;II)V
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
              public abstract bar(Landroidx/compose/Composer;II)V
            }
            public final class FooImpl implements Foo {
              public <init>()V
              public bar(Landroidx/compose/Composer;II)V
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(LFooImpl;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic LFooImpl; %tmp0_rcvr
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS FooImpl%bar%1 null null
              OUTERCLASS FooImpl bar (Landroidx/compose/Composer;II)V
            }
        """
    )

    @Test
    fun testSealedClassEtc(): Unit = checkApi(
        """
            sealed class Ambient2<T> {
                @Composable
                inline val current: T get() = error("")
                @Composable fun foo() {}
            }

            abstract class ProvidableAmbient2<T> : Ambient2<T>() {}
            class DynamicProvidableAmbient2<T> : ProvidableAmbient2<T>() {}
            class StaticProvidableAmbient2<T> : ProvidableAmbient2<T>() {}
        """,
        """
            public abstract class Ambient2 {
              private <init>()V
              public final getCurrent(Landroidx/compose/Composer;II)Ljava/lang/Object;
              public static synthetic getCurrent%annotations()V
              public final foo(Landroidx/compose/Composer;II)V
              public synthetic <init>(Lkotlin/jvm/internal/DefaultConstructorMarker;)V
              final static INNERCLASS Ambient2%foo%1 null null
            }
            final class Ambient2%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(LAmbient2;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic LAmbient2; %tmp0_rcvr
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS Ambient2%foo%1 null null
              OUTERCLASS Ambient2 foo (Landroidx/compose/Composer;II)V
            }
            public abstract class ProvidableAmbient2 extends Ambient2 {
              public <init>()V
              public static synthetic getCurrent%annotations()V
            }
            public final class DynamicProvidableAmbient2 extends ProvidableAmbient2 {
              public <init>()V
              public static synthetic getCurrent%annotations()V
            }
            public final class StaticProvidableAmbient2 extends ProvidableAmbient2 {
              public <init>()V
              public static synthetic getCurrent%annotations()V
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
              public final static getFoo(Landroidx/compose/Composer;II)I
              public static synthetic getFoo%annotations()V
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
              public final getFoo(Landroidx/compose/Composer;II)I
              public static synthetic getFoo%annotations()V
            }
        """
    )

    @Test
    fun testTableLambdaThing(): Unit = validateBytecode(
        """
        @Composable
        fun Foo() {
            val c: @Composable () -> Unit = with(123) {
                val x = @Composable {}
                x
            }
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testDefaultArgs(): Unit = validateBytecode(
        """
        @Composable
        fun Scaffold(
            topAppBar: @Composable (() -> Unit)? = null
        ) {}
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testSyntheticAccessFunctions(): Unit = validateBytecode(
        """
        class Foo {
            @Composable private fun Bar() {}
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testLambdaMemoization(): Unit = validateBytecode(
        """
        fun subcompose(block: @Composable () -> Unit) {}
        private class Foo {
            var children: @Composable (Double) -> Unit = {}
            fun subcompose() {
                val constraints = Math.random()
                subcompose {
                    children(constraints)
                }
            }
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testCustomComposerCall(): Unit = validateBytecode(
        """
        class VectorScope(val composer: VectorComposer)

        @Composable fun VectorScope.Test(children: @Composable VectorScope.() -> Unit) {
            children()
        }

        internal class AnyApplyAdapter : ApplyAdapter<Any> {
            override fun Any.start(instance: Any) {}
            override fun Any.insertAt(index: Int, instance: Any) {}
            override fun Any.removeAt(index: Int, count: Int) {}
            override fun Any.move(from: Int, to: Int, count: Int) {}
            override fun Any.end(instance: Any, parent: Any) {}
        }

        class VectorComposer(
            val root: Any,
            slotTable: SlotTable,
            recomposer: Recomposer
        ) : Composer<Any>(slotTable, Applier(root, AnyApplyAdapter()), recomposer)
        """
    ) {
        it.contains("INVOKEVIRTUAL androidx/compose/Composer.startGroup")
    }

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
              public final static getBar(Landroidx/compose/Composer;II)I
              public static synthetic getBar%annotations()V
              public final static Example(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%1 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;II)V
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
              public abstract bar(Landroidx/compose/Composer;II)V
            }
            public final class FooImpl extends BaseFoo {
              public <init>()V
              public bar(Landroidx/compose/Composer;II)V
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(LFooImpl;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic LFooImpl; %tmp0_rcvr
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS FooImpl%bar%1 null null
              OUTERCLASS FooImpl bar (Landroidx/compose/Composer;II)V
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
              public final static Wat(Landroidx/compose/Composer;II)V
              public final static Foo(ILandroidx/compose/Composer;II)V
              private final static Foo%goo(Landroidx/compose/Composer;II)V
              public final static synthetic access%Foo%goo(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Wat%1 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%Foo%goo%1 null null
            }
            final class TestKt%Wat%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Wat%1 null null
              OUTERCLASS TestKt Wat (Landroidx/compose/Composer;II)V
            }
            public final class TestKt%Foo%Bar {
              public <init>()V
              public final baz(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Foo%Bar%baz%1 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;II)V
            }
            final class TestKt%Foo%Bar%baz%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(LTestKt%Foo%Bar;I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic LTestKt%Foo%Bar; %tmp0_rcvr
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%Bar%baz%1 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              OUTERCLASS TestKt%Foo%Bar baz (Landroidx/compose/Composer;II)V
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(II)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %x
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%1 null null
              OUTERCLASS TestKt Foo (ILandroidx/compose/Composer;II)V
            }
            final class TestKt%Foo%goo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Foo%goo%1 null null
              OUTERCLASS TestKt Foo%goo (Landroidx/compose/Composer;II)V
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
              public <init>()V
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
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%J%1%1 null null
              final static INNERCLASS TestKt%J%1 null null
              OUTERCLASS TestKt J ()V
            }
            final class TestKt%J%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
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
              public final static Example(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Example%1 null null
              OUTERCLASS TestKt Example (Landroidx/compose/Composer;II)V
            }
        """
    )

    @Test
    fun testInlineCall(): Unit = checkApi(
        """
            @Composable inline fun Example(children: @Composable () -> Unit) {
                children()
            }

            @Composable fun Test() {
                Example {}
            }
        """,
        """
            public final class TestKt {
              public final static Example(Lkotlin/jvm/functions/Function3;Landroidx/compose/Composer;II)V
              public final static Test(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Test%2 null null
            }
            final class TestKt%Test%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%2 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;II)V
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
              public final static Test(Landroidx/compose/Composer;II)V
              final static INNERCLASS TestKt%Test%1%1 null null
              final static INNERCLASS TestKt%Test%4 null null
            }
            final class TestKt%Test%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function1 {
              <init>()V
              public final invoke(Landroid/widget/TextView;)V
              public synthetic bridge invoke(Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%1%1 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;II)V
            }
            final class TestKt%Test%4 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(Landroidx/compose/Composer;II)V
              private final synthetic I %%changed
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final static INNERCLASS TestKt%Test%4 null null
              OUTERCLASS TestKt Test (Landroidx/compose/Composer;II)V
            }
        """
    )

    @Test
    fun testDexNaming(): Unit = checkApi(
        """
            @Composable
            val myProperty: () -> Unit get() {
                return {  }
            }
        """,
        """
            public final class TestKt {
              public final static getMyProperty(Landroidx/compose/Composer;II)Lkotlin/jvm/functions/Function0;
              public static synthetic getMyProperty%annotations()V
              final static INNERCLASS TestKt%myProperty%1 null null
            }
            final class TestKt%myProperty%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              final static INNERCLASS TestKt%myProperty%1 null null
              OUTERCLASS TestKt getMyProperty (Landroidx/compose/Composer;II)Lkotlin/jvm/functions/Function0;
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
}