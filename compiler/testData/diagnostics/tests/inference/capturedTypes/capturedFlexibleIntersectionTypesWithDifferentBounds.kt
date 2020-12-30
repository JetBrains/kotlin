// FILE: Bar.java
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

public class Bar<K, N> { }

// FILE: Foo.java

public class Foo<P> extends Bar<Integer, Integer> {
    public static final Bar bar = null;
}

// FILE: main.kt

fun <P> takeFoo(foo: Foo<P>) {}

fun main(x: Foo<*>?) {
    val y = Foo.bar
    if (y !is Foo<*>?) return
    if (y == null) return
    if (x != y) return
    takeFoo(<!DEBUG_INFO_SMARTCAST!>x<!>) // Here we capture `{Bar<Any!, Any!> & Foo<*>}..Foo<*>?`
}
