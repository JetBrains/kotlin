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

class KtxTransformationTest : AbstractCodegenTest() {

    fun testObserveLowering() = testCompile(
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
                <FancyButton state=FancyButtonData() />
            }

            @Composable
            fun FancyButton(state: FancyButtonData) {
               <Button text=("Clicked "+state.x+" times") onClick={state.x++} id=42 />
            }
        """
    )

    fun testEmptyComposeFunction() = testCompile(
        """
        import androidx.compose.*

        class Foo {
            @Composable
            operator fun invoke() {}
        }
        """
    )

    fun testSingleViewCompose() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                <TextView />
            }
        }
        """
    )

    fun testMultipleRootViewCompose() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                <TextView />
                <TextView />
                <TextView />
            }
        }
        """
    )

    fun testNestedViewCompose() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Foo {
            @Composable
            operator fun invoke() {
                <LinearLayout>
                    <TextView />
                    <LinearLayout>
                        <TextView />
                        <TextView />
                    </LinearLayout>
                </LinearLayout>
            }
        }
        """
    )

    fun testSingleComposite() = testCompile(
        """
         import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                <Bar />
            }
        }
        """
    )

    fun testMultipleRootComposite() = testCompile(
        """
         import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                <Bar />
                <Bar />
                <Bar />
            }
        }
        """
    )

    fun testViewAndComposites() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                <LinearLayout>
                    <Bar />
                </LinearLayout>
            }
        }
        """
    )

    fun testAttributes() = testCompile(
        """
         import androidx.compose.*
        import android.widget.*

        class Bar {
            var num: Int = 0
            var a: String = ""
            var b: String = ""
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                val s = "foo" + "bar"
                <LinearLayout orientation=LinearLayout.VERTICAL>
                    <Bar num=123 a=s b="const" />
                </LinearLayout>
            }
        }
        """
    )

    // NOTE: test the key attribute separately as it receives different handling.
    // TODO(lmr): add test in compose-runtime around behavior of this attribute
    fun testKeyAttributes() = testCompile(
        """
         import androidx.compose.*

        class Foo {
            var key: Int = 0
            @Composable
            operator fun invoke() {
                <Foo key=123 />
            }
        }
        """
    )

    fun testForEach() = testCompile(
        """
         import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                listOf(1, 2, 3).forEach {
                    <Bar />
                }
            }
        }
        """
    )

    fun testForLoop() = testCompile(
        """
         import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            @Composable
            operator fun invoke() {
                for (i in listOf(1, 2, 3)) {
                    <Bar />
                }
            }
        }
        """
    )

    fun testEarlyReturns() = testCompile(
        """
         import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) return
                else "" // TODO: Remove this line when fixed upstream
                <Bar />
            }
        }
        """
    )

    fun testConditionalRendering() = testCompile(
        """
         import androidx.compose.*
        import java.util.Random

        class Bar {
            @Composable
            operator fun invoke() {}
        }

        class Bam {
            @Composable
            operator fun invoke() {}
        }

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) {
                    <Bar />
                } else {
                    <Bam />
                }
            }
        }
        """
    )

    fun testFunctionInstanceZeroArgs() = testCompile(
        """
        import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }
        class Foo {
            @Composable
            operator fun invoke() {
                val foo = object: Function0<Unit> {
                    override fun invoke() {
                        <Bar />
                    }
                }
                <foo />
            }
        }
        """
    )

    fun testFunctionInstanceMultipleArgs() = testCompile(
        """
        import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }
        class Foo {
            @Composable
            operator fun invoke() {
                val foo = object: Function2<@kotlin.ParameterName("x") String,
                  @kotlin.ParameterName("y")Int, Unit> {
                    override fun invoke(x: String, y: Int) {
                        <Bar />
                    }
                }
                <foo x="foo" y=123 />
            }
        }
        """
    )

    fun testComposeAttribute() = testCompile(
        """
        import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }
        class Foo {
            lateinit var children: () -> Unit
            @Composable
            operator fun invoke() {
                val children = children
                <children />
            }
        }
        """
    )

    fun testComposeWithParamsAttribute() = testCompile(
        """
        import androidx.compose.*

        class Bar {
            @Composable
            operator fun invoke() {}
        }
        class Foo {
            lateinit var children: (x: Int) -> Unit
            @Composable
            operator fun invoke() {
                val children = children
                <children x=123 />
            }
        }
        """
    )

    fun testComposeAttributeFunctionType() = testCompile(
        """
        import androidx.compose.*

        class X {
            lateinit var composeItem: Function1<@kotlin.ParameterName("arg0") Int, Unit>
            fun fn() {
                val composeItem = composeItem
                <composeItem arg0=123 />
            }
        }
        """
    )

    fun testExtensionFunctions() = testCompile(
        """

        import androidx.compose.*
        import android.widget.*

        fun LinearLayout.setSomeExtension(x: Int) {
        }
        class X {
            @Composable
            operator fun invoke() {
                <LinearLayout someExtension=123 />
            }
        }
        """
    )

    fun testChildrenOfComponent() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class HelperComponent {
            private lateinit var children: () -> Unit

            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                <children />
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                <HelperComponent>
                    <TextView text="some child content2!" />
                    <TextView text="some child content!3" />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenWithTypedParameters() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class HelperComponent {
            private lateinit var children: (title: String, rating: Int) -> Unit
            @Children fun setChildren2(x: (title: String, rating: Int) -> Unit) { children = x }

            @Composable
            operator fun invoke() {
                val children = this.children
                <children title="Hello World!" rating=5 />
                <children title="Kompose is awesome!" rating=5 />
                <children title="Bitcoin!" rating=4 />
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                <HelperComponent> title: String, rating: Int ->
                    <TextView text=(title+" ("+rating+" stars)") />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenWithUntypedParameters() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class HelperComponent {
            private lateinit var children: (title: String, rating: Int) -> Unit

            @Children
            fun setChildren2(x: (title: String, rating: Int) -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                <children title="Hello World!" rating=5 />
                <children title="Kompose is awesome!" rating=5 />
                <children title="Bitcoin!" rating=4 />
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                <HelperComponent> title, rating ->
                    <TextView text=(title+" ("+rating+" stars)") />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenCaptureVariables() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class HelperComponent {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                <HelperComponent>
                    <TextView text=childText />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenDeepCaptureVariables() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class A {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class B {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                <A>
                    <B>
                        println(childText)
                    </B>
                </A>
            }
        }
        """
    )

    fun testChildrenDeepCaptureVariablesWithParameters() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class A {
            lateinit private var children: (String) -> Unit
            @Children
            fun setChildren2(x: (String) -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class B {
            lateinit private var children: (String) -> Unit
            @Children
            fun setChildren2(x: (String) -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                <A> x ->
                    <B> y ->
                        println(childText + x + y)
                    </B>
                </A>
            }
        }
        """
    )

    fun testChildrenOfNativeView() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class MainComponent {
            @Composable
            operator fun invoke() {
                <LinearLayout>
                    <TextView text="some child content2!" />
                    <TextView text="some child content!3" />
                </LinearLayout>
            }
        }
        """
    )

    fun testIrSpecial() = testCompile(
        """
        import android.widget.*
        import androidx.compose.*

        class HelperComponent {
            private lateinit var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            @Composable
            operator fun invoke() {
                val children = this.children
            }
        }

        class MainComponent {
            @Composable
            operator fun invoke() {
                val x = "Hello"
                val y = "World"
                <HelperComponent>
                    for(i in 1..100) {
                        <TextView text=(x+y+i) />
                    }
                    Unit // NOTE(lmr): this Unit is needed here but it's a general compiler bug,
                         // not our bug. Remove when fixed.
                </HelperComponent>
            }
        }
        """
    )

    fun testForLoopIrBug() = testCompile(
        """
        var z = {
            for (i in 1..100) {
                print("wat")
            }
            Unit // NOTE(lmr): this Unit is needed here but it's a general compiler bug,
                 // not our bug. Remove when fixed.
        }
        """
    )

    fun testGenericsInnerClass() = testCompile(
        """
        import androidx.compose.*

        class A<T>(val value: T) {
            inner class Getter {
                var x: T? = null
                @Composable
                operator fun invoke() {}
            }
        }

        fun doStuff() {
            val a = A(123)

            // a.Getter() here has a bound type argument through A
            <a.Getter x=456 />
        }
        """
    )

    fun testXGenericInnerClassConstructor() = testCompile(
        """
        import androidx.compose.*

        class A<T>(val value: T) {
            inner class C {
                @Composable
                operator fun invoke() {}
            }
        }

        fun doStuff() {
            val B = A(123)

            <B.C />
        }
        """
    )

    fun testXGenericConstructorParams() = testCompile(
        """
        import androidx.compose.*

        class A<T>(
            val value: T
        ) {
            var list2: List<T>? = null
            fun setList(list: List<T>) {}
            @Composable
            operator fun invoke() {}
        }

        fun doStuff() {
            val x = 123

            // we can create element with just value, no list
            <A value=x />

            // if we add a list, it can infer the type
            <A
                value=x
                list=listOf(234, x)
                list2=listOf(234, x)
            />
        }
        """
    )

    fun testSimpleNoArgsComponent() = testCompile(
        """
        import androidx.compose.*

        class Simple {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <Simple />
        }
        """
    )

    fun testSimpleVarInConstructor() = testCompile(
        """
        import androidx.compose.*

        class SimpleConstructorArg(var foo: String) {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <SimpleConstructorArg foo="string" />
        }
        """
    )

    fun testLateInitProp() = testCompile(
        """
        import androidx.compose.*

        class SimpleLateInitArg {
            lateinit var foo: String
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <SimpleLateInitArg foo="string" />
        }
        """
    )

    fun testGenericCtorArg() = testCompile(
        """
        import androidx.compose.*

        class GenericCtorArg<T>(var foo: T) {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <GenericCtorArg foo="string" />
            <GenericCtorArg foo=123 />
        }
        """
    )

    fun testPropsAndSettersAndExtensionSetters() = testCompile(
        """
        import androidx.compose.*

        class OneArg {
            var bar: String? = null
            var baz: String? = null
            fun setBam(bam: String) {
                bar = bam
            }
            @Composable
            operator fun invoke() {}
        }

        fun OneArg.setJazz(x: String) {}

        fun OneArg.setJazz(y: Int) {}

        fun run() {
            <OneArg bar="string" />
            val bar = "string"
            val num = 123
            <OneArg
                bar
                baz=bar
                bam=bar
                jazz=num
            />
        }
        """
    )

    fun testGenericAttribute() = testCompile(
        """
        import androidx.compose.*

        class Simple {
            @Composable
            operator fun invoke() {}
        }

        class Ref<T> {
            var value: T? = null
        }

        fun <T: Any> T.setRef(ref: Ref<T>) {

        }

        fun run() {
            val ref = Ref<Simple>()
            <Simple ref=ref />
        }
        """
    )

    fun testSimpleFunctionComponent() = testCompile(
        """
        import androidx.compose.*

        fun OneArg(foo: String) {}

        fun run() {
            <OneArg foo="string" />
        }
        """
    )

    fun testOverloadedFunctions() = testCompile(
        """
        import androidx.compose.*

        fun OneArg(foo: String) {}
        fun OneArg(foo: Int) {}

        fun run() {
            <OneArg foo=("string") />
            <OneArg foo=123 />
        }
        """
    )

    fun testConstructorVal() = testCompile(
        """
        import androidx.compose.*

        class Foo(val x: Int) {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <Foo x=123 />
        }
        """
    )

    fun testConstructorNonPropertyParam() = testCompile(
        """
        import androidx.compose.*

        class Foo(x: Int) {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            <Foo x=123 />
        }
        """
    )

    fun testDotQualifiedObjectToClass() = testCompile(
        """
        import androidx.compose.*

        object Obj {
            class B {
                @Composable
                operator fun invoke() {}
            }
        }

        fun run() {
            <Obj.B />
        }
        """
    )

    fun testPackageQualifiedTags() = testCompile(
        """
        import androidx.compose.*

        fun run() {
            <android.widget.TextView text="bar" />
        }
        """
    )

    fun testDotQualifiedClassToClass() = testCompile(
        """
        import androidx.compose.*

        class Y {
            class Z {
                class F {
                    @Composable
                    operator fun invoke() {}
                }
            }
        }

        fun run() {
            <Y.Z.F />
        }
        """
    )

    fun testInnerClass() = testCompile(
        """
        import androidx.compose.*

        class A(var foo: String) {
            inner class B(var bar: String) {
                @Composable
                operator fun invoke() {}
            }
        }

        fun run() {
            val X = A("string")
            <X.B bar="string" />
        }
        """
    )

    fun testGenericInnerClass() = testCompile(
        """
        import androidx.compose.*

        class A<T>(var foo: T) {
            inner class B(var bar: T) {
                @Composable
                operator fun invoke() {}
            }
        }

        fun run() {
            val X = A("string")
            val Y = A(123)
            <X.B bar="string" />
            <Y.B bar=123 />
        }
        """
    )

    fun testLocalLambda() = testCompile(
        """
        import androidx.compose.*

        class Simple {
            @Composable
            operator fun invoke() {}
        }

        fun run() {
            val foo = { <Simple /> }
            <foo />
        }
        """
    )

    fun testPropertyLambda() = testCompile(
        """
        import androidx.compose.*

        class Test(var children: () -> Unit) {
            @Composable
            operator fun invoke() {
                <children />
            }
        }
        """
    )

    fun testLambdaWithArgs() = testCompile(
        """
        import androidx.compose.*

        class Test(var children: (x: Int) -> Unit) {
            @Composable
            operator fun invoke() {
                <children x=123 />
            }
        }
        """
    )

    fun testLocalMethod() = testCompile(
        """
        import androidx.compose.*

        class Test {
            fun doStuff() {}
            @Composable
            operator fun invoke() {
                <doStuff />
            }
        }
        """
    )

    fun testPunningProperty() = testCompile(
        """
        import androidx.compose.*

        class Simple(var foo: String) {
            fun setBar(bar: String) {}
            @Composable
            operator fun invoke() {}
        }

        class Test(var foo: String, var bar: String) {
            @Composable
            operator fun invoke() {
                <Simple foo bar />
            }
        }
        """
    )

    fun testPunningLocalVar() = testCompile(
        """
        import androidx.compose.*

        class Simple() {
            var bar: String? = null
            fun setFoo(foo: String) {}
            @Composable
            operator fun invoke() {}
        }

        class Test {
            @Composable
            operator fun invoke() {
                val foo = "string"
                val bar = "other"
                <Simple foo bar />
            }
        }
        """
    )

    fun testSimpleLambdaChildrenSetter() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*
        import android.content.*

        class Example {
            @Children
            fun setChildren(fn: () -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    // disabled: b/133441299
    fun xtestSimpleLambdaChildrenProperty() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*
        import android.content.*

        class Example {
            @Children
            var children: (() -> Unit)? = null
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    fun testSimpleLambdaChildrenPropertyInCtor() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*
        import android.content.*

        class Example(
            @Children var children: () -> Unit
        ) {
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    fun testBlockChildrenForViews() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        fun run(text: String) {
            <LinearLayout>
                println("hello ${"$"}text")
            </LinearLayout>
        }
        """
    )

    fun testChildrenLambdaSetterWithSingleParam() = testCompile(
        """
        import androidx.compose.*

        class Example {
            @Children
            fun setChildren(fn: (x: Int) -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example> x ->
                println("hello ${"$"}x")
            </Example>
        }
        """
    )

    // disabled: b/133441299
    fun xtestChildrenLambdaPropertyWithSingleParam() = testCompile(
        """
        import androidx.compose.*

        class Example {
            @Children
            var children: ((x: Int) -> Unit)? = null
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example> x ->
                println("hello ${"$"}x")
            </Example>
        }
        """
    )

    fun testChildrenLambdaSetterWithMultipleParams() = testCompile(
        """
        import androidx.compose.*

        class Example {
            @Children
            fun setChildren(fn: (x: Int, y: String) -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {

//                val lambda = { x, y -> ... }
//                composer.call(
//                    123,
//                    { changed(lambda) },
//                    { Example().also { setChildren(lambda) }() }
//                )


            <Example> x, y ->
                println("hello ${"$"}x ${"$"}y")
            </Example>
        }
        """
    )

    // disabled: b/133441299
    fun xtestChildrenLambdaPropertyWithMultipleParams() = testCompile(
        """
        import androidx.compose.*

        class Example {
            @Children
            var children: ((x: Int, y: String) -> Unit)? = null
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example> x, y ->
                println("hello ${"$"}x ${"$"}y")
            </Example>
        }
        """
    )

    fun testGenericChildrenArgSetter() = testCompile(
        """
        import androidx.compose.*

        class Example<T>(var value: T) {
            @Children
            fun setChildren(fn: (x: T) -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example value="string"> x ->
                println("hello ${"$"}x")
            </Example>
            <Example value=123> x ->
                println("hello ${"$"}{x + 1}")
            </Example>
        }
        """
    )

    // disabled b/133441299
    fun xtestGenericChildrenArgProperty() = testCompile(
        """
        import androidx.compose.*

        class Example<T>(var value: T) {
            @Children
            var children: ((x: T) -> Unit)? = null
            @Composable
            operator fun invoke() {}
        }

        fun run(text: String) {
            <Example value="string"> x ->
                println("hello ${"$"}x")
            </Example>
            <Example value=123> x ->
                println("hello ${"$"}{x + 1}")
            </Example>
        }
        """
    )

    fun testFunctionComponentsWithChildrenSimple() = testCompile(
        """
        import androidx.compose.*

        fun Example(@Children children: () -> Unit) {}

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    fun testFunctionComponentWithChildrenOneArg() = testCompile(
        """
        import androidx.compose.*

        fun Example(@Children children: (String) -> Unit) {}

        fun run(text: String) {
            <Example> x ->
                println("hello ${"$"}x")
            </Example>
        }
        """
    )

    // disabled: b/133441299
    fun xtestFunctionComponentWithGenericChildren() = testCompile(
        """
        import androidx.compose.*

        fun <T> Example(foo: T, @Children children: (T) -> Unit) {}

        fun run(text: String) {
            <Example foo="string"> x ->
                println("hello ${"$"}x")
            </Example>
            <Example foo=123> x ->
                println("hello ${"$"}{x + 1}")
            </Example>
        }
        """
    )

    fun testKtxLambdaInForLoop() = testCompile(
        """
        import androidx.compose.*
        import android.widget.TextView

        fun foo() {
            val lambda = @Composable {  }
            for(x in 1..5) {
                <lambda />
                <lambda />
            }
        }
        """
    )

    fun testKtxLambdaInIfElse() = testCompile(
        """
        import androidx.compose.*
        import android.widget.TextView

        fun foo(x: Boolean) {
            val lambda = @Composable { <TextView text="Hello World" /> }
            if(true) {
                <lambda />
                <lambda />
                <lambda />
            } else {
                <lambda />
            }
        }
        """
    )

    fun testLateUsingObjectLiteral() = testCompile(
        """
        import androidx.compose.*

         class Example {
             lateinit var callback: (Int) -> Unit
             var index = 0
             @Composable
            operator fun invoke() {
               <Example callback=(object : Function1<Int, Unit> {
                    override fun invoke(p1: Int) {
                        index = p1
                    }
                }) />
             }
         }
        """
    )

    fun testMultiplePivotalAttributesOdd() = testCompile(
        """
        import androidx.compose.*

        class Foo(
            val a: Int,
            val b: Int,
            val c: Int,
            val d: Int,
            val e: Int
        ) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    a=1
                    b=2
                    c=3
                    d=4
                    e=5
                />
            }
        }
        """
    )

    fun testMultiplePivotalAttributesEven() = testCompile(
        """
        import androidx.compose.*

        class Foo(
            val a: Int,
            val b: Int,
            val c: Int,
            val d: Int
        ) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    a=1
                    b=2
                    c=3
                    d=4
                />
            }
        }
        """
    )

    fun testSinglePivotalAttribute() = testCompile(
        """
        import androidx.compose.*

        class Foo(
            val a: Int
        ) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    a=1
                />
            }
        }
        """
    )

    fun testKeyAttributeWithPivotal() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Foo(
            val a: Int,
            var b: Int
        ) {
            @Composable
            operator fun invoke() {}
        }

        fun Foo.setKey(@Pivotal key: Any?) {}

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    a=1
                    b=2
                    key=3
                />
            }
        }
        """
    )

    fun testKeyAttributeWithoutPivotal() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        @Pivotal
        fun Foo.setKey(x: Any?) {}

        class Foo(
            var a: Int,
            var b: Int
        ) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    a=1
                    b=2
                    key=3
                />
            }
        }
        """
    )

    fun testNamedChildrenAttributeProperty() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Foo {
            @Children var children: (() -> Unit)? = null
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    children={ }
                />
            }
        }
        """
    )

    fun testNamedChildrenAttributeSetter() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Foo {
            @Children fun setChildren(children: () -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo
                    children={ }
                />
            }
        }
        """
    )

    // disabled: b/133441299
    fun xtestOverloadedChildren() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Foo {
            @Children var children: ((x: Int, y: Int) -> Unit)? = null
            @Children fun setChildren(children: (x: Int) -> Unit) {}
            @Children fun setChildren(children: () -> Unit) {}
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo children={ -> } />
                <Foo children={ x -> println(x) } />
                <Foo children={ x, y -> println(x + y) } />
                <Foo>
                    println("")
                </Foo>
                <Foo> x ->
                    println(x)
                </Foo>
                <Foo> x, y ->
                    println(x + y)
                </Foo>
            }
        }
        """
    )

    fun testKtxVariableTagsProperlyCapturedAcrossKtxLambdas() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Foo(@Children var children: (sub: () -> Unit) -> Unit) {
            @Composable
            operator fun invoke() {}
        }

        class Boo(@Children var children: () -> Unit) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                <Foo> sub ->
                    <Boo>
                        <sub />
                    </Boo>
                </Foo>
            }
        }
        """
    )

    // disabled: b/133441299
    fun xtestPassChildrenLambdaVarWithCorrectType() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Boo(@Children var children: () -> Unit) {
            @Composable
            operator fun invoke() {}
        }

        class Bar(var data: List<Int>) {
            @Composable
            operator fun invoke() {
                val children = { Unit; }
                <Boo children />
            }
        }
        """
    )

    fun testPassChildrenLambdaLiteralWithCorrectType() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Boo {
            @Children lateinit var children: () -> Unit
            @Composable
            operator fun invoke() {}
        }

        class Bar(var data: List<Int>) {
            @Composable
            operator fun invoke() {
                <Boo
                    children={ Unit; }
                />
            }
        }
        """
    )

    // disabled: b/133441299
    fun xtestChildrenLiteral() = testCompile(
        """
        import androidx.compose.*
        import androidx.ui.androidview.adapters.*

        class Boo(@Children var children: () -> Unit) {
            @Composable
            operator fun invoke() {}
        }

        class Bar(var data: List<Int>) {
            @Composable
            operator fun invoke() {
                <Boo
                    children={ Unit; }
                />
            }
        }
        """
    )

    fun testKtxLambdaCapturedVariableInAssignment() = testCompile(
        """
        import androidx.compose.*

        class Tabs(@Children var children: () -> Unit) {
            @Composable
            operator fun invoke() {}
        }

        class Bar {
            @Composable
            operator fun invoke() {
                val bam = "x"
                <Tabs>
                    val qoo = bam
                </Tabs>
            }
        }
        """
    )

    fun testKtxParameterlessFunction() = testCompile(
        """
        import androidx.compose.*
        import android.widget.*

        @Composable
        fun Paramless() {
          <TextView text="Hello!" />
        }

        class Bar {
          @Composable
            operator fun invoke() {
            <Paramless />
          }
        }
        """
    )

    fun testKtxEmittable() = testCompile(
        """
        import androidx.compose.*

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
            operator fun invoke() {
            <MyEmittable a=2 />
          }
        }
        """
    )

    fun testKtxCompoundEmittable() = testCompile(
        """
        import androidx.compose.*

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
            operator fun invoke() {
            <MyEmittable a=1>
              <MyEmittable a=2 />
              <MyEmittable a=3 />
              <MyEmittable a=4 />
              <MyEmittable a=5 />
            </MyEmittable>
          }
        }
        """
    )

    fun testInvocableObject() = testCompile(
        """
        import androidx.compose.*

        class Foo { }
        @Composable
        operator fun Foo.invoke() {  }

        @Composable
        fun test() {
            val foo = Foo()
            <foo />
        }
        """
    )
}