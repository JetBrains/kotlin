// FULL_JDK
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

// FILE: Bar.java

public class Bar<K, N> { }

// FILE: Foo.java

import java.util.List;

public class Foo<P> extends Bar<Integer, Integer> {
    public static final List<?> bar = null;
}

// FILE: main.kt

fun <P> takeFoo(foo: Foo<P>) {}

fun main(x: Foo<*>?) {
    val y = Foo.bar
    if (y !is Foo<*>?) return
    if (y == null) return
    if (x != y) return
    // Here we capture `({Foo<*> & MutableList<*>}..{Foo<*>? & List<*>?})`
    // `*` inside `MutableList` and `List` have to become the same captured type
    takeFoo(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
