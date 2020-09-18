// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -REDUNDANT_PROJECTION

// FILE: Foo.java
public class Foo<L> extends Bar<L> {
    public Foo(L x) {
        super(x);
    }
}

// FILE: main.kt
fun <T : Any> Bar<T>.foo(): T = null as T
fun <T : Any> Bar<T?>.bar(): T = null as T
fun <T : Any> Foo<T>.boo1(): T = null as T
fun <T : Any> Foo<T?>.boo2(): T = null as T

open class Bar<out K>(val x: K)

fun main(x: Foo<out Number?>, y: Bar<out Number?>, z1: Foo<out Number>, z2: Bar<out Number>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x.foo()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x.bar()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x.boo1()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo2<!>()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>y.bar()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo1<!>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>y.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo2<!>()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Number..kotlin.Number?)")!>z1.foo()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z1.bar()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z1.boo1()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>z1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo2<!>()<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z2.foo()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z2.bar()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>z2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo1<!>()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>z2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>boo2<!>()<!>
}
