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

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.Composer
import androidx.compose.currentComposerNonNull
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.reflect.KClass

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposeCallLoweringTests : AbstractCodegenTest() {

    @Test
    fun testInlineGroups(): Unit = forComposerParam(/*true, */false) {
        compose("""

            @Composable
            fun App() {
                val cond = state { true }
                val text = if (cond.value) remember { "abc" } else remember { "def" }
                Button(id=1, text=text, onClick={ cond.value = !cond.value })
            }
        """,
            "App()"
        ).then { activity ->
            val tv = activity.findViewById<Button>(1)
            assertEquals("abc", tv.text)
            tv.performClick()
        }.then { activity ->
            val tv = activity.findViewById<Button>(1)
            assertEquals("def", tv.text)
        }
    }

    @Test
    fun testSimpleInlining(): Unit = forComposerParam(true, false) {
        compose("""
            @Composable
            inline fun foo(block: @Composable() () -> Unit) {
                block()
            }

            @Composable
            fun App() {
                foo {}
            }
        """,
            "App()"
        )
    }

    @Test
    fun testComposableLambdaCall(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*

                @Composable
                fun test(children: @Composable() () -> Unit) {
                    children()
                }
            """
        )
    }

    @Test
    fun testProperties(): Unit = forComposerParam(true, false) {
        codegen(
            """
            import androidx.compose.*

            @Composable val foo get() = 123

            class A {
                @Composable val bar get() = 123
            }

            @Composable val A.bam get() = 123

            @Composable fun Foo() {
            }

            @Composable
            fun test() {
                val a = A()
                foo
                Foo()
                a.bar
                a.bam
            }
        """
        )
    }

    @Test
    fun testPropertyValues(): Unit = forComposerParam(true, false) {
        compose("""
            @Composable val foo get() = "123"

            class A {
                @Composable val bar get() = "123"
            }

            @Composable val A.bam get() = "123"

            @Composable
            fun App() {
                val a = A()
                TextView(id=1, text=a.bar)
                TextView(id=2, text=foo)
                TextView(id=3, text=a.bam)
            }
        """,
            "App()"
        ).then { activity ->
            fun assertText(id: Int, value: String) {
                val tv = activity.findViewById<TextView>(id)
                assertEquals(value, tv.text)
            }
            assertText(1, "123")
            assertText(2, "123")
            assertText(3, "123")
        }
    }

    @Test
    fun testComposableLambdaCallWithGenerics(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*

                @Composable fun <T> A(value: T, block: @Composable() (T) -> Unit) {
                    block(value)
                }

                @Composable fun <T> B(
                    value: T,
                    block: @Composable() (@Composable() (T) -> Unit) -> Unit
                ) {
                    block({ })
                }

                @Composable
                fun test() {
                    A(123) { it ->
                        println(it)
                    }
                    B(123) { it ->
                        it(456)
                    }
                }
            """
        )
    }

    @Test
    fun testMethodInvocations(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*

                val x = Ambient.of<Int> { 123 }

                @Composable
                fun test() {
                    x.Provider(456) {

                    }
                }
            """
        )
    }

    @Test
    fun testReceiverLambdaInvocation(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*

                class TextSpanScope internal constructor(val composer: ViewComposer)

                @Composable fun TextSpanScope.Foo(children: @Composable TextSpanScope.() -> Unit) {
                    children()
                }
            """
        )
    }

    @Test
    fun testReceiverLambda2(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*

                class DensityScope(val density: Density)

                class Density

                val DensityAmbient = Ambient.of<Density>()

                @Composable
                fun ambientDensity() = ambient(DensityAmbient)

                @Composable
                fun WithDensity(block: @Composable DensityScope.() -> Unit) {
                    DensityScope(ambientDensity()).block()
                }
            """
        )
    }

    @Test
    fun testInlineChildren(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import androidx.compose.*
                import android.widget.LinearLayout

                @Composable
                inline fun PointerInputWrapper(
                    crossinline children: @Composable() () -> Unit
                ) {
                    // Hide the internals of PointerInputNode
                    LinearLayout {
                        children()
                    }
                }
            """
        )
    }

    @Test
    fun testNoComposerImport(): Unit = forComposerParam(true, false) {
        codegenNoImports(
            """
        import androidx.compose.Composable
        import android.widget.LinearLayout

        @Composable
        fun Foo() {
            // emits work
            LinearLayout {
                // nested calls work
                Bar()
            }
            // calls work
            Bar()
        }

        @Composable
        fun Bar() {}

            """.trimIndent()
        )
    }

    @Test
    fun testInlineNoinline(): Unit = forComposerParam(true, false) {
        codegen(
            """
        @Composable
        inline fun PointerInputWrapper(
            crossinline children: @Composable() () -> Unit
        ) {
            LinearLayout {
                children()
            }
        }

        @Composable
        fun PressReleasedGestureDetector(children: @Composable() () -> Unit) {
            PointerInputWrapper {
                children()
            }
        }
            """.trimIndent()
        )
    }

    @Test
    fun testInlinedComposable(): Unit = forComposerParam(true, false) {
        codegen(
            """
        @Composable 
        inline fun Foo(crossinline children: @Composable() () -> Unit) {
                children()
        }

        @Composable fun test(children: @Composable() () -> Unit) {
            Foo {
                println("hello world")
                children()
            }
        }
            """
        )
    }

    @Test
    fun testGenericEmittables(): Unit = forComposerParam(true, false) {
        codegen(
            """
        class FooKey<T>(val name: String)
        class Foo<T>(val key: FooKey<T>, var value: T): Emittable {
            override fun emitInsertAt(index: Int, instance: Emittable) {

            }

            override fun emitRemoveAt(index: Int, count: Int) {

            }

            override fun emitMove(from: Int, to: Int, count: Int) {

            }
        }

        val AnyKey = FooKey<Any>("any")

        @Composable fun test(value: Any, children: @Composable() () -> Unit) {
            Foo(key=AnyKey, value=value) {
                children()
            }
        }
            """
        )
    }

    @Test
    fun testSetViewContentIssue(): Unit = forComposerParam(true, false) {
        codegen(
            """
                import android.app.Activity
                import android.os.Bundle
                import android.view.Gravity
                import android.view.ViewGroup
                import android.widget.*
                import androidx.compose.*
                import androidx.ui.androidview.adapters.*

                class RippleActivity : Activity() {

                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setViewContent {
                            val layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                0,
                                1f
                            )
                            val gravity = Gravity.CENTER_HORIZONTAL
                            LinearLayout(orientation = LinearLayout.VERTICAL) {
                                TextView(gravity = gravity, text = "Compose card with ripple:")
                                LinearLayout(layoutParams = layoutParams) {
                                    // RippleDemo()
                                }
                                TextView(gravity = gravity, text = "Platform button with ripple:")
                                LinearLayout(layoutParams = layoutParams, padding = 50.dp) {
                                    // Button(background = getDrawable(R.drawable.ripple))
                                }
                            }
                        }
                    }
                }
            """
        )
    }

    @Test
    fun testGenericParameterOrderIssue(): Unit = forComposerParam(true, false) {
        codegen(
            """
@Composable
fun A() {
    val x = ""
    val y = ""

    B(bar = x, foo = y)
}

@Composable
fun <T> B(foo: T, bar: String) { }
            """
        )
    }

    @Test
    fun testArgumentOrderIssue(): Unit = forComposerParam(true, false) {
        codegen(
            """
                class A

                @Composable
                fun B() {
                    C(bar = 1, foo = 1f)
                }

                @Composable
                fun C(
                    foo: Float,
                    bar: Int
                ) {

                }
            """
        )
    }

    @Test
    fun testObjectName(): Unit = forComposerParam(true, false) {
        codegen(
            """

            @Composable fun SomeThing(children: @Composable() () -> Unit) {}

            @Composable
            fun Example() {
                SomeThing {
                    val id = object {}
                }
            }
            """
        )
    }

    @Test
    fun testWebViewBug(): Unit = forComposerParam(true, false) {
        codegen(
            """
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.Composable
import androidx.compose.composer

class WebContext {
    var webView: WebView? = null
}

private fun WebView.setRef(ref: (WebView) -> Unit) {
    ref(this)
}

@Composable
fun WebComponent(
    url: String,
    webViewClient: WebViewClient = WebViewClient(),
    webContext: WebContext
) {

    WebView(
        ref = { webContext.webView = it }
    )
}
            """
        )
    }

    @Test
    fun testStuffThatIWantTo(): Unit = forComposerParam(true, false) {
        codegen(
            """

            fun startCompose(block: @Composable() () -> Unit) {}

            fun nonComposable() {
                startCompose {
                    LinearLayout {

                    }
                }
            }
            """
        )
    }

    @Test
    fun testSimpleFunctionResolution(): Unit = forComposerParam(true, false) {
        compose(
            """
            import androidx.compose.*

            @Composable
            fun noise(text: String) {}

            @Composable
            fun bar() {
                noise(text="Hello World")
            }
            """,
            """
            """
        )
    }

    @Test
    fun testSimpleClassResolution(): Unit = forComposerParam(true, false) {
        compose(
            """
            import android.widget.TextView
            import androidx.compose.*

            @Composable
            fun bar() {
                TextView(text="Hello World")
            }
            """,
            """
            """
        )
    }

    @Test
    fun testSetContent(): Unit = forComposerParam(true, false) {
        codegen(
            """
                fun fakeCompose(block: @Composable() ()->Unit) { }

                class Test {
                    fun test() {
                        fakeCompose {
                            LinearLayout(orientation = LinearLayout.VERTICAL) {}
                        }
                    }
                }
            """
        )
    }

    @Test
    fun testComposeWithResult(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable fun <T> identity(block: @Composable() ()->T): T = block()

                @Composable
                fun TestCall() {
                  val value: Any = identity { 12 }
                  TextView(text = value.toString(), id = 100)
                }
            """,
            "TestCall()"
        ).then { activity ->
            val textView = activity.findViewById<TextView>(100)
            assertEquals("12", textView.text)
        }
    }

    @Test
    fun testObservable(): Unit = forComposerParam(true, false) {
        compose(
            """
                import android.widget.Button
                import androidx.compose.*
                import androidx.ui.androidview.adapters.setOnClick

                @Model
                class FancyButtonData() {
                    var x = 0
                }

                @Composable
                fun SimpleComposable() {
                    FancyButton(state=FancyButtonData())
                }

                @Composable
                fun FancyButton(state: FancyButtonData) {
                    Button(text=("Clicked "+state.x+" times"), onClick={state.x++}, id=42)
                }
            """,
            "SimpleComposable()"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Clicked 3 times", button.text)
        }
    }

    @Test // java.lang.ClassNotFoundException: Z
    fun testObservableLambda(): Unit = forComposerParam(/*true, */false) {
        compose(
            """
                @Model
                class FancyButtonCount() {
                    var count = 0
                }

                @Composable
                fun SimpleComposable(state: FancyButtonCount) {
                    FancyBox2 {
                        Button(
                          text=("Button clicked "+state.count+" times"),
                          onClick={state.count++},
                          id=42
                        )
                    }
                }

                @Composable
                fun FancyBox2(children: @Composable() ()->Unit) {
                    children()
                }
            """,
            "SimpleComposable(state=remember { FancyButtonCount() })"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    fun testObservableGenericFunction(): Unit = forComposerParam(/*true, */false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            @Composable
            fun <T> SimpleComposable(state: Counter, value: T) {
                Button(
                  text=("Button clicked "+state.count+" times: " + value),
                  onClick={state.count++},
                  id=42
                )
            }
        """,
            "SimpleComposable(state=remember { Counter() }, value=\"Value\")"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times: Value", button.text)
        }
    }

    @Test
    fun testObservableExtension(): Unit = forComposerParam(true, false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            @Composable
            fun Counter.Composable() {
                Button(
                    text="Button clicked "+count+" times",
                    onClick={count++},
                    id=42
                )
            }

            val myCounter = Counter()
            """,
            "myCounter.Composable()"
        ).then { activity ->
            val button = activity.findViewById<Button>(42)
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    fun testObserverableExpressionBody(): Unit = forComposerParam(true, false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            @Composable
            fun SimpleComposable(counter: Counter) =
                Button(
                    text="Button clicked "+counter.count+" times",
                    onClick={counter.count++},
                    id=42
                )

            @Composable
            fun SimpleWrapper(counter: Counter) = SimpleComposable(counter = counter)

            val myCounter = Counter()
            """,
            "SimpleWrapper(counter = myCounter)"
        ).then { activity ->
            val button = activity.findViewById<Button>(42)
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    fun testObservableInlineWrapper(): Unit = forComposerParam(true, false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            var inWrapper = false
            val counter = Counter()

            inline fun wrapper(block: () -> Unit) {
              inWrapper = true
              try {
                block()
              } finally {
                inWrapper = false
              }
            }

            @Composable
            fun SimpleComposable(state: Counter) {
                wrapper {
                    Button(
                      text=("Button clicked "+state.count+" times"),
                      onClick={state.count++},
                      id=42
                    )
                }
            }
        """,
            "SimpleComposable(state=counter)"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    fun testObservableDefaultParameter(): Unit = forComposerParam(true, false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            val counter = Counter()

            @Composable
            fun SimpleComposable(state: Counter, a: Int = 1, b: Int = 2) {
                Button(
                  text=("State: ${'$'}{state.count} a = ${'$'}a b = ${'$'}b"),
                  onClick={state.count++},
                  id=42
                )
            }
        """,
            "SimpleComposable(state=counter, b = 4)"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("State: 3 a = 1 b = 4", button.text)
        }
    }

    @Test
    fun testObservableEarlyReturn(): Unit = forComposerParam(true, false) {
        compose("""
            @Model
            class Counter() {
                var count = 0
            }

            val counter = Counter()

            @Composable
            fun SimpleComposable(state: Counter) {
                Button(
                  text=("State: ${'$'}{state.count}"),
                  onClick={state.count++},
                  id=42
                )

                if (state.count > 2) return

                TextView(
                  text="Included text",
                  id=43
                )
            }
        """,
            "SimpleComposable(state=counter)"
        ).then { activity ->
            // Check that the text view is in the view
            assertNotNull(activity.findViewById(43))
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("State: 3", button.text)

            // Assert that the text view is no longer in the view
            assertNull(activity.findViewById<Button>(43))
        }
    }

    @Test
    fun testCGSimpleTextView(): Unit = forComposerParam(true, false) {
        compose(
            """

            """,
            """
                TextView(text="Hello, world!", id=42)
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testCGLocallyScopedFunction(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo() {
                    @Composable fun Bar() {
                        TextView(text="Hello, world!", id=42)
                    }
                    Bar()
                }
            """,
            """
                Foo()
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testCGLocallyScopedExtensionFunction(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(x: String) {
                    @Composable fun String.Bar() {
                        TextView(text=this, id=42)
                    }
                    x.Bar()
                }
            """,
            """
                Foo(x="Hello, world!")
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testImplicitReceiverScopeCall(): Unit = forComposerParam(true, false) {
        compose(
            """
                import androidx.compose.*

                class Bar(val text: String)

                @Composable fun Bar.Foo() {
                    TextView(text=text,id=42)
                }

                @Composable
                fun Bam(bar: Bar) {
                    with(bar) {
                        Foo()
                    }
                }
            """,
            """
                Bam(bar=Bar("Hello, world!"))
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testCGLocallyScopedInvokeOperator(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(x: String) {
                    @Composable
                    operator fun String.invoke() {
                        TextView(text=this, id=42)
                    }
                    x()
                }
            """,
            """
                Foo(x="Hello, world!")
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testTrivialExtensionFunction(): Unit = forComposerParam(true, false) {
        compose(
            """ """,
            """
                val x = "Hello"
                @Composable fun String.foo() {}
                x.foo()
            """
        )
    }

    @Test
    fun testTrivialInvokeExtensionFunction(): Unit = forComposerParam(true, false) {
        compose(
            """ """,
            """
                val x = "Hello"
                @Composable operator fun String.invoke() {}
                x()
            """
        )
    }

    @Test
    fun testCGNSimpleTextView(): Unit = forComposerParam(true, false) {
        compose(
            """

            """,
            """
                TextView(text="Hello, world!", id=42)
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test // java.lang.ClassNotFoundException: Z
    fun testInliningTemp(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(x: Double, children: @Composable Double.() -> Unit) {
                  x.children()
                }
            """,
            """
                Foo(x=1.0) {
                    TextView(text=this.toString(), id=123)
                }
            """,
            { mapOf("foo" to "bar") }
        ).then { activity ->
            val textView = activity.findViewById(123) as TextView
            assertEquals("1.0", textView.text)
        }
    }

    @Test
    fun testInliningTemp2(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(onClick: Double.() -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then { }
    }

    @Test
    fun testInliningTemp3(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(onClick: (Double) -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then { }
    }

    @Test
    fun testInliningTemp4(): Unit = forComposerParam(true, false) {
        compose(
            """
                @Composable
                fun Foo(onClick: (Double) -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then {}
    }

    @Test
    fun testInline_NonComposable_Identity(): Unit = forComposerParam(true, false) {
        compose("""
            @Composable inline fun InlineWrapper(base: Int, children: @Composable() ()->Unit) {
              children()
            }
            """,
            """
            InlineWrapper(200) {
              TextView(text = "Test", id=101)
            }
            """).then { activity ->
            assertEquals("Test", activity.findViewById<TextView>(101).text)
        }
    }

    @Test
    fun testInline_Composable_Identity(): Unit = forComposerParam(true, false) {
        compose("""
            """,
            """
              TextView(text="Test", id=101)
            """).then { activity ->
            assertEquals("Test", activity.findViewById<TextView>(101).text)
        }
    }

    @Test
    fun testInline_Composable_EmitChildren(): Unit = forComposerParam(true, false) {
        compose("""
            @Composable
            inline fun InlineWrapper(base: Int, crossinline children: @Composable() ()->Unit) {
              LinearLayout(id = base + 0) {
                children()
              }
            }
            """,
            """
            InlineWrapper(200) {
              TextView(text = "Test", id=101)
            }
            """).then { activity ->
            val tv = activity.findViewById<TextView>(101)
            // Assert the TextView was created with the correct text
            assertEquals("Test", tv.text)
            // and it is the first child of the linear layout
            assertEquals(tv, activity.findViewById<LinearLayout>(200).getChildAt(0))
        }
    }

    @Test // java.lang.ClassNotFoundException: Z
    fun testCGNInlining(): Unit = forComposerParam(true, false) {
        compose(
            """

            """,
            """
                LinearLayout(orientation=LinearLayout.VERTICAL) {
                    TextView(text="Hello, world!", id=42)
                }
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    fun codegen(text: String, dumpClasses: Boolean = false): Unit {
        codegenNoImports(
            """
           import android.content.Context
           import android.widget.*
           import androidx.compose.*

           $text

        """, dumpClasses)
    }

    fun codegenNoImports(text: String, dumpClasses: Boolean = false) {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }

    fun compose(
        supportingCode: String,
        composeCode: String,
        valuesFactory: () -> Map<String, Any> = { emptyMap() },
        dumpClasses: Boolean = false
    ): RobolectricComposeTester {
        val className = "TestFCS_${uniqueNumber++}"
        val fileName = "$className.kt"

        val candidateValues = valuesFactory()

        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        val parameterList = candidateValues.map {
            if (it.key.contains(':')) {
                it.key
            } else "${it.key}: ${it.value::class.qualifiedName}"
        }.joinToString()
        val parameterTypes = candidateValues.map {
            it.value::class.javaPrimitiveType ?: it.value::class.javaObjectType
        }.toTypedArray()

        val compiledClasses = classLoader(
            """
       import android.content.Context
       import android.widget.*
       import androidx.compose.*
       import androidx.ui.androidview.adapters.*

       $supportingCode

       class $className {

         @Composable
         fun test($parameterList) {
           $composeCode
         }
       }
    """, fileName, dumpClasses
        )

        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val testMethod = if (ComposeFlags.COMPOSER_PARAM)
            instanceClass.getMethod("test", *parameterTypes, Composer::class.java)
        else
            instanceClass.getMethod("test", *parameterTypes)

        return compose {
            val values = valuesFactory()
            val arguments = values.map { it.value as Any }.toTypedArray()
            if (ComposeFlags.COMPOSER_PARAM) {
                testMethod.invoke(instanceOfClass, *arguments, currentComposerNonNull)
            } else {
                testMethod.invoke(instanceOfClass, *arguments)
            }
        }
    }

    private fun ResolvedCall<*>.isEmit(): Boolean = candidateDescriptor is ComposableEmitDescriptor
    private fun ResolvedCall<*>.isCall(): Boolean =
        candidateDescriptor is ComposableFunctionDescriptor

    private val callPattern = Regex("(<normal>)|(<emit>)|(<call>)")
    private fun extractCarets(text: String): Pair<String, List<Pair<Int, String>>> {
        val indices = mutableListOf<Pair<Int, String>>()
        var offset = 0
        val src = callPattern.replace(text) {
            indices.add(it.range.first - offset to it.value)
            offset += it.range.last - it.range.first + 1
            ""
        }
        return src to indices
    }

    private fun resolvedCallAtOffset(
        bindingContext: BindingContext,
        jetFile: KtFile,
        index: Int
    ): ResolvedCall<*>? {
        val element = jetFile.findElementAt(index)!!
        val callExpression = element.parentOfType<KtCallExpression>()
        return callExpression?.getResolvedCall(bindingContext)
    }
}

private inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

private fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
    return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}