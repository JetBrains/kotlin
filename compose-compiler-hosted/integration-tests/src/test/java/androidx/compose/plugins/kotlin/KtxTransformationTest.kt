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

import org.junit.Ignore

class KtxTransformationTest : AbstractCodegenTest() {

    fun testObserveLowering() = ensureSetup { testCompile(
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
        """
    ) }

    fun testEmptyComposeFunction() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class Foo {
            @Composable
            operator fun invoke() {}
        }
        """
    ) }

    fun testSingleViewCompose() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                TextView()
            }
        }
        """
    ) }

    fun testMultipleRootViewCompose() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                TextView()
                TextView()
                TextView()
            }
        }
        """
    ) }

    fun testNestedViewCompose() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                LinearLayout {
                    TextView()
                    LinearLayout {
                        TextView()
                        TextView()
                    }
                }
            }
        }
        """
    ) }

    fun testSingleComposite() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                Bar()
            }
        }
        """
    ) }

    fun testMultipleRootComposite() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                Bar()
                Bar()
                Bar()
            }
        }
        """
    ) }

    fun testViewAndComposites() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                LinearLayout {
                    Bar()
                }
            }
        }
        """
    ) }

    fun testForEach() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                listOf(1, 2, 3).forEach {
                    Bar()
                }
            }
        }
        """
    ) }

    fun testForLoop() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                for (i in listOf(1, 2, 3)) {
                    Bar()
                }
            }
        }
        """
    ) }

    fun testEarlyReturns() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) return
                else "" // TODO: Remove this line when fixed upstream
                Bar()
            }
        }
        """
    ) }

    fun testConditionalRendering() = ensureSetup { testCompile(
        """
         import androidx.compose.*

        @Composable
        fun Bar() {}

        @Composable
        fun Bam() {}

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) {
                    Bar()
                } else {
                    Bam()
                }
            }
        }
        """
    ) }

    fun testExtensionFunctions() = ensureSetup { testCompile(
        """

        import androidx.compose.*
        import android.widget.*

        fun LinearLayout.setSomeExtension(x: Int) {
        }
        class X {
            @Composable
            operator fun invoke() {
                LinearLayout(someExtension=123)
            }
        }
        """
    ) }

    fun testChildrenWithTypedParameters() = ensureSetup { testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        @Composable fun HelperComponent(
            children: @Composable (title: String, rating: Int) -> Unit
        ) {
            children("Hello World!", 5)
            children("Kompose is awesome!", 5)
            children("Bitcoin!", 4)
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                HelperComponent { title: String, rating: Int ->
                    TextView(text=(title+" ("+rating+" stars)"))
                }
            }
        }
        """
    ) }

    fun testChildrenCaptureVariables() = ensureSetup { testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        @Composable fun HelperComponent(children: @Composable () -> Unit) {
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                HelperComponent {
                    TextView(text=childText + name)
                }
            }
        }
        """
    ) }

    fun testChildrenDeepCaptureVariables() = ensureSetup { testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        @Composable fun A(children: @Composable () -> Unit) {
            children()
        }

        @Composable fun B(children: @Composable () -> Unit) {
            children()
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                A {
                    B {
                        println(childText + name)
                    }
                }
            }
        }
        """
    ) }

    fun testChildrenDeepCaptureVariablesWithParameters() = ensureSetup {
        testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        @Composable fun A(children: @Composable (x: String) -> Unit) {
            children("")
        }

        @Composable fun B(children: @Composable (y: String) -> Unit) {
            children("")
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                A { x ->
                    B { y ->
                        println(childText + name + x + y)
                    }
                }
            }
        }
        """
        )
    }

    fun testChildrenOfNativeView() = ensureSetup { testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class MainComponent {
            @Composable
            operator fun invoke() {
                LinearLayout {
                    TextView(text="some child content2!")
                    TextView(text="some child content!3")
                }
            }
        }
        """
    ) }

    fun testIrSpecial() = ensureSetup { testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        @Composable fun HelperComponent(children: @Composable () -> Unit) {}

        class MainComponent {
            @Composable
            operator fun invoke() {
                val x = "Hello"
                val y = "World"
                HelperComponent {
                    for(i in 1..100) {
                        TextView(text=x+y+i)
                    }
                }
            }
        }
        """
    ) }

    fun testGenericsInnerClass() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class A<T>(val value: T) {
            @Composable fun Getter(x: T? = null) {
            }
        }

        @Composable
        fun doStuff() {
            val a = A(123)

            // a.Getter() here has a bound type argument through A
            a.Getter(x=456)
        }
        """
    ) }

    fun testXGenericConstructorParams() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable fun <T> A(
            value: T,
            list: List<T>? = null
        ) {

        }

        @Composable
        fun doStuff() {
            val x = 123

            // we can create element with just value, no list
            A(value=x)

            // if we add a list, it can infer the type
            A(
                value=x,
                list=listOf(234, x)
            )
        }
        """
    ) }

    fun testSimpleNoArgsComponent() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable
        fun Simple() {}

        @Composable
        fun run() {
            Simple()
        }
        """
    ) }

    fun testDotQualifiedObjectToClass() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        object Obj {
            @Composable
            fun B() {}
        }

        @Composable
        fun run() {
            Obj.B()
        }
        """
    ) }

    fun testPackageQualifiedTags() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable
        fun run() {
            android.widget.TextView(text="bar")
        }
        """
    ) }

    fun testLocalLambda() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable
        fun Simple() {}

        @Composable
        fun run() {
            val foo = @Composable { Simple() }
            foo()
        }
        """
    ) }

    fun testPropertyLambda() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class Test(var children: @Composable () () -> Unit) {
            @Composable
            operator fun invoke() {
                children()
            }
        }
        """
    ) }

    fun testLambdaWithArgs() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class Test(var children: @Composable (x: Int) -> Unit) {
            @Composable
            operator fun invoke() {
                children(x=123)
            }
        }
        """
    ) }

    fun testLocalMethod() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class Test {
            @Composable
            fun doStuff() {}
            @Composable
            operator fun invoke() {
                doStuff()
            }
        }
        """
    ) }

    fun testSimpleLambdaChildren() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.*
        import android.content.*

        @Composable fun Example(children: @Composable () -> Unit) {

        }

        @Composable
        fun run(text: String) {
            Example {
                println("hello ${"$"}text")
            }
        }
        """
    ) }

    fun testFunctionComponentsWithChildrenSimple() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable
        fun Example(children: @Composable () -> Unit) {}

        @Composable
        fun run(text: String) {
            Example {
                println("hello ${"$"}text")
            }
        }
        """
    ) }

    fun testFunctionComponentWithChildrenOneArg() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        @Composable
        fun Example(children: @Composable (String) -> Unit) {}

        @Composable
        fun run(text: String) {
            Example { x ->
                println("hello ${"$"}x")
            }
        }
        """
    ) }

    fun testKtxLambdaInForLoop() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.TextView

        @Composable
        fun foo() {
            val lambda = @Composable {  }
            for(x in 1..5) {
                lambda()
                lambda()
            }
        }
        """
    ) }

    fun testKtxLambdaInIfElse() = ensureSetup { testCompile(
        """
        import androidx.compose.*
        import android.widget.TextView

        @Composable
        fun foo(x: Boolean) {
            val lambda = @Composable { TextView(text="Hello World") }
            if(true) {
                lambda()
                lambda()
                lambda()
            } else {
                lambda()
            }
        }
        """
    ) }

    fun testKtxVariableTagsProperlyCapturedAcrossKtxLambdas() = ensureSetup {
        testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        @Composable fun Foo(children: @Composable (sub: @Composable () -> Unit) -> Unit) {

        }

        @Composable fun Boo(children: @Composable () -> Unit) {

        }

        class Bar {
            @Composable
            operator fun invoke() {
                Foo { sub ->
                    Boo {
                        sub()
                    }
                }
            }
        }
        """
        )
    }

    fun testKtxEmittable() = ensureSetup { testCompileEmittable(
        """

        open class MockEmittable: Emittable {
          override fun emitInsertAt(index: Int, instance: Emittable) {}
          override fun emitRemoveAt(index: Int, count: Int) {}
          override fun emitMove(from: Int, to: Int, count: Int) {}
        }

        class MyEmittable: MockEmittable() {
          var a: Int = 1
        }

        class Comp {
            @Composable
            fun Example() {
                MyEmittable(a=2)
            }
        }
        """
    ) }

    @Ignore("b/150394471")
    fun xtestKtxCompoundEmittable() = ensureSetup { testCompileEmittable(
        """
        open class MockEmittable: Emittable {
          override fun emitInsertAt(index: Int, instance: Emittable) {}
          override fun emitRemoveAt(index: Int, count: Int) {}
          override fun emitMove(from: Int, to: Int, count: Int) {}
        }

        class MyEmittable: MockEmittable() {
          var a: Int = 1
        }

        @Composable fun Test() {
            MyEmittable(a=1) {
                MyEmittable(a=2)
                MyEmittable(a=3)
                MyEmittable(a=4)
                MyEmittable(a=5)
            }
        }
        """
    ) }

    fun testInvocableObject() = ensureSetup { testCompile(
        """
        import androidx.compose.*

        class Foo { }
        @Composable
        operator fun Foo.invoke() {  }

        @Composable
        fun test() {
            val foo = Foo()
            foo()
        }
        """
    ) }
}