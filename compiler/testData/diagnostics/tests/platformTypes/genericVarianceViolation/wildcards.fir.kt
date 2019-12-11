// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<?> x) {}
    void foo(Iterable<?> x) {}
    void foo(Iterator<?> x) {}
    void foo(Set<?> x) {}
    void foo(Map<?, ?> x) {}
    void foo(Map.Entry<?, ?> x) {}
}

// FILE: main.kt

fun main(
        a: A,
        ml: MutableList<String>, l: List<String>,
        ms: MutableSet<String>, s: Set<String>,
        mm: MutableMap<Any, String>, m: Map<Any, String>,
        mme: MutableMap.MutableEntry<Any, String>, me: Map.Entry<Any, String>
) {
    // Lists
    a.foo(ml)
    a.foo(l)

    // Iterables
    val mit: MutableIterable<String> = ml
    val it: Iterable<String> = ml
    a.foo(mit)
    a.foo(it)

    // Iterators
    a.foo(ml.iterator())
    a.foo(l.iterator())

    // Sets
    a.foo(ms)
    a.foo(s)

    // Maps
    a.foo(mm)
    a.foo(m)

    // Map entries
    a.foo(mme)
    a.foo(me)
}
