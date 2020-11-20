// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -REDUNDANT_PROJECTION
// !LANGUAGE: +InferenceCompatibility

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
    x.foo()
    x.bar()
    x.boo1()
    x.<!INAPPLICABLE_CANDIDATE!>boo2<!>()

    y.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    y.bar()
    y.<!INAPPLICABLE_CANDIDATE!>boo1<!>()
    y.<!INAPPLICABLE_CANDIDATE!>boo2<!>()

    z1.foo()
    z1.bar()
    z1.boo1()
    z1.<!INAPPLICABLE_CANDIDATE!>boo2<!>()

    z2.foo()
    z2.bar()
    z2.<!INAPPLICABLE_CANDIDATE!>boo1<!>()
    z2.<!INAPPLICABLE_CANDIDATE!>boo2<!>()
}
