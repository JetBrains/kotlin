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

class KtxTypeResolutionTests : AbstractComposeDiagnosticsTest() {

    fun testImplicitlyPassedReceiverScope1() = doTest(
        """
            import androidx.compose.*

            @Composable
            fun Int.Foo(@Children children: Int.() -> Unit) {
                <children />
            }
        """
    )

    fun testImplicitlyPassedReceiverScope2() = doTest(
        """
            import androidx.compose.*

            @Composable
            fun Int.Foo(@Children children: Int.(foo: String) -> Unit) {
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>children<!> />
            }

            @Composable
            fun Bar(@Children children: Int.() -> Unit) {
                <<!NO_VALUE_FOR_PARAMETER!>children<!> />
            }
        """
    )

    fun testThatUnresolvedTagDiagnosticIsOnlyOnTagName() = doTest(
        """
            import androidx.compose.*

            class Foo {
                @Composable operator fun invoke() {}
            }

            @Composable fun test() {
                <<!UNRESOLVED_TAG!>SomeNameThatWillNotResolve<!> <!UNRESOLVED_ATTRIBUTE_KEY!>foo<!>=123>
                    <Foo />
                </<!INVALID_TAG_DESCRIPTOR!>SomeNameThatWillNotResolve<!>>
            }
        """
    )

    fun testAmbiguousKtxTags() = doTest(
        """
            import androidx.compose.*

            class Foo {
                var foo: Int = 0
                @Composable operator fun invoke() {}
            }

            @Composable
            fun Foo(foo: Int) { print(foo) }

            @Composable fun test() {
                <<!AMBIGUOUS_KTX_CALL!>Foo<!> foo=0 />
            }
        """
    )

    fun testSmartCastsAndPunning() = doTest(
        """
            import androidx.compose.*

            @Composable
            fun Foo(bar: String) { print(bar) }

            @Composable
            fun test(bar: String?) {
                <Foo <!TYPE_MISMATCH!>bar<!> />
                if (bar != null) {
                    <Foo bar />
                    <Foo bar=bar />
                }
            }
        """
    )

    fun testExtensionInvoke() = doTest(
        """
            import androidx.compose.*

            class Foo {}
            @Composable operator fun Foo.invoke() {}

            @Composable fun test() {
                <Foo />
            }
        """
    )

    fun testResolutionInsideWhenExpression() = doTest(
        """
            import androidx.compose.*
            import android.widget.TextView

            @Composable fun doSomething(foo: Boolean) {
                when (foo) {
                    true -> <TextView text="Started..." />
                    false -> <TextView text="Continue..." />
                }
            }
        """
    )

    fun testComposerExtensions() = doTest(
        """
            import androidx.compose.*

            open class Foo {}
            class Bar : Foo() {}

            class Bam {}

            fun <T : Foo> ViewComposition.emit(key: Any, ctor: () -> T, update: ViewUpdater<T>.() -> Unit) {
                print(key)
                print(ctor)
                print(update)
            }

            @Composable fun test() {
                <Bar />
                <<!INVALID_TAG_TYPE!>Bam<!> />
            }
        """
    )

    fun testUsedParameters() = doTest(
        """
            import androidx.compose.*
            import android.widget.LinearLayout

            class Foo {
                var composeItem: @Composable() () -> Unit = {}
                @Composable operator fun invoke(x: Int) {
                    println(x)
                }
            }


            @Composable fun test(
                @Children children: @Composable() () -> Unit,
                value: Int,
                x: Int,
                @Children children2: @Composable() () -> Unit,
                value2: Int
            ) {
                <LinearLayout>
                    // attribute value
                    <Foo x=value />

                    // punned attribute
                    <Foo x />

                    // tag
                    <children />
                </LinearLayout>
                <Foo x=123 composeItem={
                    val abc = 123

                    // attribute value
                    <Foo x=abc />

                    // attribute value
                    <Foo x=value2 />

                    // tag
                    <children2 />
                } />
            }
        """
    )

    fun testDispatchInvoke() = doTest(
        """
            import androidx.compose.*

            class Bam {
                @Composable fun Foo() {}
            }

            @Composable fun test() {
                with(Bam()) {
                    <Foo />
                }
            }
        """
    )

    fun testDispatchAndExtensionReceiver() = doTest(
        """
            import androidx.compose.*

            class Bam {
                inner class Foo {}
            }

            @Composable operator fun Bam.Foo.invoke() {}

            @Composable fun test() {
                with(Bam()) {
                    <Foo />
                }
            }
        """
    )

    fun testDispatchAndExtensionReceiverLocal() = doTest(
        """
            import androidx.compose.*


            class Foo {}

            class Bam {
                @Composable operator fun Foo.invoke() {}
                @Composable operator fun invoke() {
                    <Foo />
                }
            }

        """
    )

    fun testMissingAttributes() = doTest(
        """
            import androidx.compose.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }
            class B(var x: Foo) { @Composable operator fun invoke() { println(x) } }
            class C(x: Foo) { init { println(x) } @Composable operator fun invoke() { } }
            class D(val x: Foo) { @Composable operator fun invoke() { println(x) } }
            class E {
                lateinit var x: Foo
                @Composable operator fun invoke() { println(x) }
            }

            // NOTE: It's important that the diagnostic be only over the tag target, and not the entire element
            // so that a single error doesn't end up making a huge part of an otherwise correct file "red".
            @Composable fun Test(F: @Composable() (x: Foo) -> Unit) {
                // NOTE: constructor attributes and fn params get a "missing parameter" diagnostic
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>A<!> />
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>B<!> />
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>C<!> />
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>D<!> />

                // NOTE: lateinit attributes get only the "missing required attribute" diagnostic
                <<!MISSING_REQUIRED_ATTRIBUTES!>E<!> />

                // local
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>F<!> />

                val x = Foo(123)

                <A x />
                <B x />
                <C x />
                <D x />
                <E x />
                <F x />
            }

        """.trimIndent()
    )

    fun testDuplicateAttributes() = doTest(
        """
            import androidx.compose.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }
            class B(var x: Foo) { @Composable operator fun invoke() { println(x) } }
            class C(x: Foo) { init { println(x) } @Composable operator fun invoke() { } }
            class D(val x: Foo) { @Composable operator fun invoke() { println(x) } }
            class E {
                lateinit var x: Foo
                @Composable operator fun invoke() { println(x) }
            }

            @Composable fun Test() {
                val x = Foo(123)

                // NOTE: It's important that the diagnostic be only over the attribute key, so that
                // we don't make a large part of the elements red when the type is otherwise correct
                <A x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <B x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <C x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <D x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <E x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
            }

        """.trimIndent()
    )

    fun testChildrenNamedAndBodyDuplicate() = doTest(
        """
            import androidx.compose.*

            @Composable fun A(@Children children: @Composable() () -> Unit) { <children /> }
            class B(@Children var children: @Composable() () -> Unit) { @Composable operator fun invoke() { <children /> } }
            class C {
                @Children var children: @Composable() () -> Unit = {}
                @Composable operator fun invoke() { <children /> }
            }
            class D {
                @Children lateinit var children: @Composable() () -> Unit
                @Composable operator fun invoke() { <children /> }
            }

            @Composable fun Test() {
                <A <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></A>
                <B <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></B>
                <C <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></C>
                <D <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></D>
            }

        """.trimIndent()
    )

    fun testAbstractClassTags() = doTest(
        """
            import androidx.compose.*
            import android.content.Context
            import android.widget.LinearLayout

            abstract class Foo {}

            abstract class Bar(context: Context) : LinearLayout(context) {}

            @Composable fun Test() {
                <<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, INVALID_TAG_TYPE!>Foo<!> />
                <<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Bar<!> />
            }

        """.trimIndent()
    )

    fun testAmbiguousTags() = doTest(
        """
            import androidx.compose.*

            @Composable fun Foo() {}
            @Composable fun Foo(x: Int) { println(x) }


            class Wat {
                operator fun invoke(): Xam {
                    return Xam()
                }
            }

            class Xam {
                @Composable
                operator fun invoke(x: Int) { println(x) }
            }

            @Composable fun Test() {
//                <Foo x=123 />
//                <Foo />
                <Wat x=123 />
            }

        """.trimIndent()
    )

    fun testOverloadedTargets() = doTest(
        """
            import androidx.compose.*

            data class FooModel(val x: Int, val y: Double)

            class Foo(model: FooModel) {
                init { println(model) }
                constructor(x: Int, y: Double) : this(FooModel(x, y))

                @Composable operator fun invoke() {}
            }


            @Composable fun Bar(x: Int, y: Double) { <Bar model=FooModel(x, y) /> }
            @Composable fun Bar(model: FooModel) { println(model) }

            @Composable fun Test() {
                val x = 1
                val y = 1.0
                val model = FooModel(x, y)

                <Foo x y />
                <Foo model />
                <Foo x y <!UNRESOLVED_ATTRIBUTE_KEY!>model<!> />

                <Bar x y />
                <Bar model />
                <Bar x y <!UNRESOLVED_ATTRIBUTE_KEY!>model<!> />
            }

        """.trimIndent()
    )

    fun testGenerics() = doTest(
        """
            import androidx.compose.*

            class A { fun a() {} }
            class B { fun b() {} }

            class Foo<T>(var value: T, var f: (T) -> Unit) {
                @Composable operator fun invoke() {}
            }

            @Composable fun <T> Bar(x: Int, value: T, f: (T) -> Unit) { println(value); println(f); println(x) }

            @Composable fun Test() {

                val fa: (A) -> Unit = { it.a() }
                val fb: (B) -> Unit = { it.b() }

                <Foo value=A() f={ it.a() } />
                <Foo value=B() f={ it.b() } />
                <Foo value=A() f=fa />
                <Foo value=B() f=fb />
                <Foo value=B() f={ it.<!UNRESOLVED_REFERENCE!>a<!>() } />
                <Foo value=A() f={ it.<!UNRESOLVED_REFERENCE!>b<!>() } />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Foo<!> <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>value<!>=A() <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>f<!>=fb />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Foo<!> <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>value<!>=B() <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>f<!>=fa />


                <Bar x=1 value=A() f={ it.a() } />
                <Bar x=1 value=B() f={ it.b() } />
                <Bar x=1 value=A() f=fa />
                <Bar x=1 value=B() f=fb />
                <Bar x=1 value=B() f={ it.<!UNRESOLVED_REFERENCE!>a<!>() } />
                <Bar x=1 value=A() f={ it.<!UNRESOLVED_REFERENCE!>b<!>() } />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Bar<!> x=1 <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>value<!>=A() <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>f<!>=fb />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Bar<!> x=1 <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>value<!>=B() <!MISMATCHED_INFERRED_ATTRIBUTE_TYPE!>f<!>=fa />
            }

        """.trimIndent()
    )

    fun testUnresolvedAttributeValueResolvedTarget() = doTest(
        """
            import androidx.compose.*

            class Foo {
                var bar: Int = 0
                @Composable operator fun invoke(x: Int) {
                    print(x)
                }
            }

            @Composable fun Fam(bar: Int, x: Int) {
                print(bar)
                print(x)
            }

            @Composable fun Test() {
                <Foo <!MISMATCHED_ATTRIBUTE_TYPE!>bar<!>=<!UNRESOLVED_REFERENCE!>undefined<!> x=1 />
                <Foo bar=1 x=<!UNRESOLVED_REFERENCE!>undefined<!> />
                <Foo <!UNRESOLVED_REFERENCE, MISMATCHED_ATTRIBUTE_TYPE!>bar<!> <!UNRESOLVED_REFERENCE!>x<!> />
                <Fam bar=<!UNRESOLVED_REFERENCE!>undefined<!> x=1 />
                <Fam bar=1 x=<!UNRESOLVED_REFERENCE!>undefined<!> />
                <Fam <!UNRESOLVED_REFERENCE!>bar<!> <!UNRESOLVED_REFERENCE!>x<!> />

                <Fam <!MISMATCHED_ATTRIBUTE_TYPE!>bar<!>=<!TYPE_MISMATCH!>""<!> <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=<!TYPE_MISMATCH!>""<!> />
            }

        """.trimIndent()
    )

    fun testEmptyAttributeValue() = doTest(
        """
            import androidx.compose.*

            @Composable fun Foo(abc: Int, xyz: Int) {
                print(abc)
                print(xyz)
            }

            @Composable fun Test() {
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_ATTRIBUTES!>Foo<!> abc= />

                // NOTE(lmr): even though there is NO diagnostic here, there *is* a parse
                // error. This is intentional and done to mimic how kotlin handles function
                // calls with no value expression in a call parameter list (ie, `Foo(123,)`)
                <Foo abc=123 xyz= />
            }

        """.trimIndent()
    )

    fun testMissingComposer() = doTest(
        """
            import androidx.compose.Composable

            @Composable fun A() {}
            @Composable fun B(a: Int, b: Int) { print(a + b) }
            fun C(a: Int): @Composable() (b: Int) -> Unit = { b: Int -> print(a + b) }
            class D(var a: Int) {
                var b: Int = 0
                @Composable operator fun invoke(c: Int) { print(c) }
            }

            class E(var a: Int) {
                var b: Int = 0
            }

            @Composable fun Test() {
                <<!NO_COMPOSER_FOUND!>A<!> />

                <<!NO_COMPOSER_FOUND, NO_VALUE_FOR_PARAMETER!>B<!> />
                <<!NO_COMPOSER_FOUND, NO_VALUE_FOR_PARAMETER!>B<!> a=1 />
                <<!NO_COMPOSER_FOUND!>B<!> a=1 b=1 />
                <<!NO_COMPOSER_FOUND!>B<!> a=1 <!MISMATCHED_ATTRIBUTE_TYPE!>b<!>=<!TYPE_MISMATCH!>""<!> />

                <<!NO_COMPOSER_FOUND, NO_VALUE_FOR_PARAMETER!>C<!> />
                <<!NO_COMPOSER_FOUND!>C<!> a=1 />
                <<!NO_COMPOSER_FOUND!>C<!> a=1 b=1 />
                <<!NO_COMPOSER_FOUND!>C<!> a=1 <!MISMATCHED_ATTRIBUTE_TYPE!>b<!>=<!TYPE_MISMATCH!>""<!> />

                <<!NO_COMPOSER_FOUND, NO_VALUE_FOR_PARAMETER!>D<!> />
                <<!NO_COMPOSER_FOUND!>D<!> a=1 />
                <<!NO_COMPOSER_FOUND!>D<!> a=1 b=1 />
                <<!NO_COMPOSER_FOUND!>D<!> a=1 b=1 c=1 />
                <<!NO_COMPOSER_FOUND!>D<!> a=1 b=1 <!MISMATCHED_ATTRIBUTE_TYPE!>c<!>=<!TYPE_MISMATCH!>""<!> />

                <<!NO_COMPOSER_FOUND, NO_VALUE_FOR_PARAMETER!>E<!> />
                <<!NO_COMPOSER_FOUND!>E<!> a=1 />
                <<!NO_COMPOSER_FOUND!>E<!> a=1 b=1 />
                <<!NO_COMPOSER_FOUND!>E<!> a=1 <!MISMATCHED_ATTRIBUTE_TYPE!>b<!>="" />
            }

        """.trimIndent()
    )

    fun testValidInvalidAttributes() = doTest(
        """
            import androidx.compose.*

            class Foo(val a: Int, var b: Int, c: Int, d: Int = 1) {
                init { println(c); println(d); }
                var e = 1
                var f: Int? = null
                var g: Int
                    get() = 1
                    set(_: Int) {}
                val h: Int get() = 1
                val i = 1

                fun setJ(j: Int) { println(j) }

                val k by lazy { 123 }
                private var l = 1
                private var m: Int
                    get() = 1
                    set(v: Int) { println(v) }
                private val n = 1
                @Composable operator fun invoke() {}
            }

            @Composable fun Test() {
                <Foo
                    a=1
                    b=1
                    c=1
                    d=1
                    e=1
                    f=null
                    g=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>h<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>i<!>=1
                    j=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>k<!>=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>z<!>=1
                    <!INVISIBLE_MEMBER!>l<!>=1
                    <!INVISIBLE_MEMBER!>m<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>n<!>=1
                />
            }

        """.trimIndent()
    )

    fun testMismatchedAttributes() = doTest(
        """
            import androidx.compose.*

            open class A {}
            class B : A() {}

            class Foo() {
                var x: A = A()
                var y: A = B()
                var z: B = B()
                @Composable operator fun invoke() {}
            }

            @Composable fun Test() {
                <Foo
                    x=A()
                    y=A()
                    <!MISMATCHED_ATTRIBUTE_TYPE!>z<!>=A()
                />
                <Foo
                    x=B()
                    y=B()
                    z=B()
                />
                <Foo
                    <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>y<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>z<!>=1
                />
            }

        """.trimIndent()
    )

    fun testErrorAttributeValue() = doTest(
        """
            import androidx.compose.*

            class Foo() {
                var x: Int = 1
                @Composable operator fun invoke() {}
            }

            @Composable fun Test() {
                <Foo
                    <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
                    <!UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE!>y<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
                />
            }

        """.trimIndent()
    )

    fun testUnresolvedQualifiedTag() = doTest(
        """
            import androidx.compose.*

            object MyNamespace {
                class Foo() {
                    @Children var children: @Composable() () -> Unit = {}
                    @Composable operator fun invoke() { <children /> }
                }
                @Composable fun Bar(@Children children: @Composable() () -> Unit = {}) { <children /> }

                var Baz = @Composable { }

                var someString = ""
                class NonComponent {}
            }

            class Boo {
                class Wat {
                    @Children var children: @Composable() () -> Unit = {}
                    @Composable operator fun invoke() { <children /> }
                }
                inner class Qoo {
                    @Children var children: @Composable() () -> Unit = {}
                    @Composable operator fun invoke() { <children /> }
                }
            }

            @Composable fun Test() {

                <MyNamespace.Foo />
                <MyNamespace.Bar />
                <MyNamespace.Baz />
                <<!INVALID_TAG_DESCRIPTOR!>MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!><!> />
                <<!INVALID_TAG_TYPE!>MyNamespace.<!UNRESOLVED_REFERENCE!>someString<!><!> />
                <<!INVALID_TAG_TYPE!>MyNamespace.NonComponent<!> />
                <MyNamespace.Foo></MyNamespace.Foo>
                <MyNamespace.Bar></MyNamespace.Bar>
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>MyNamespace.Baz<!>></MyNamespace.Baz>


                val obj = Boo()
                <obj.Qoo />
                <Boo.Wat />
                <obj.Qoo></obj.Qoo>
                <Boo.Wat></Boo.Wat>
                <<!INVALID_TAG_DESCRIPTOR!>obj.<!UNRESOLVED_REFERENCE!>Wat<!><!> />

                <<!INVALID_TAG_DESCRIPTOR!>MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!><!> />
                <<!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo />

                <<!INVALID_TAG_DESCRIPTOR!>obj.<!UNRESOLVED_REFERENCE!>Wat<!><!>>
                </<!INVALID_TAG_DESCRIPTOR!>obj.Wat<!>>

                <<!INVALID_TAG_DESCRIPTOR!>MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!><!>>
                </<!INVALID_TAG_DESCRIPTOR!>MyNamespace.Qoo<!>>

                <<!INVALID_TAG_TYPE!>MyNamespace.<!UNRESOLVED_REFERENCE!>someString<!><!>>
                </<!INVALID_TAG_TYPE!>MyNamespace.someString<!>>

                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!><!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo<!>>
                </SomethingThatDoesntExist.Foo>

                <<!INVALID_TAG_TYPE!>MyNamespace.NonComponent<!>>
                </<!INVALID_TAG_TYPE!>MyNamespace.NonComponent<!>>

                <<!INVALID_TAG_DESCRIPTOR!>MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!><!>>
                </<!INVALID_TAG_DESCRIPTOR!>MyNamespace.Bam<!>>

            }

        """.trimIndent()
    )

    fun testExtensionAttributes() = doTest(
        """
            import androidx.compose.*

            class Foo() {
                var x: Int = 1
                @Composable operator fun invoke() {}
            }

            fun Foo.setBar(x: Int) { println(x) }

            fun Foo.setX(s: String) { println(s) }

            @Composable fun Test() {
                <Foo
                    x=1
                />

                <Foo
                    x="a"
                />

                <Foo
                    bar=123
                />

                <Foo
                    <!MISMATCHED_ATTRIBUTE_TYPE!>bar<!>=123.0
                    <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=123.0
                />
            }

        """.trimIndent()
    )

    fun testChildren() = doTest(
        """
            import androidx.compose.*
            import android.widget.Button
            import android.widget.LinearLayout

            class ChildrenRequired1(@Children var children: @Composable() () -> Unit) {
                @Composable operator fun invoke() {}
            }

            @Composable fun ChildrenRequired2(@Children children: @Composable() () -> Unit) { <children /> }

            class ChildrenOptional1(@Children var children: @Composable() () -> Unit = {}) {
                @Composable operator fun invoke() {}
            }
            class ChildrenOptional2() {
                @Children var children: @Composable() () -> Unit = {}
                @Composable operator fun invoke() {}
            }

            @Composable fun ChildrenOptional3(@Children children: @Composable() () -> Unit = {}) { <children /> }

            class NoChildren1() {
                @Composable operator fun invoke() {}
            }
            @Composable fun NoChildren2() {}


            class MultiChildren() {
                @Children var c1: @Composable() () -> Unit = {}
                @Children var c2: @Composable() (x: Int) -> Unit = {}
                @Children var c3: @Composable() (x: Int, y: Int) -> Unit = { x, y -> println(x + y) }

                @Composable operator fun invoke() {}
            }

            @Composable fun Test() {
                <ChildrenRequired1></ChildrenRequired1>
                <ChildrenRequired2></ChildrenRequired2>
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_CHILDREN!>ChildrenRequired1<!> />
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_CHILDREN!>ChildrenRequired2<!> />

                <ChildrenOptional1></ChildrenOptional1>
                <ChildrenOptional2></ChildrenOptional2>
                <ChildrenOptional3></ChildrenOptional3>
                <ChildrenOptional1 />
                <ChildrenOptional2 />
                <ChildrenOptional3 />


                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>NoChildren1<!>></NoChildren1>
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>NoChildren2<!>></NoChildren2>
                <NoChildren1 />
                <NoChildren2 />

                <MultiChildren></MultiChildren>
                <MultiChildren> x ->
                    println(x)
                </MultiChildren>
                <MultiChildren> x, y ->
                    println(x + y)
                </MultiChildren>
                <<!UNRESOLVED_CHILDREN!>MultiChildren<!>> <!TYPE_MISMATCH!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>, <!CANNOT_INFER_PARAMETER_TYPE!>z<!><!> ->
                    println(x + y + z)<!>
                </MultiChildren>

                <Button />
                <LinearLayout />

                <LinearLayout>
                </LinearLayout>

                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>Button<!>></Button>
            }

        """.trimIndent()
    )
}
