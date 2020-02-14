// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
    void foo(Iterable<Object> x) {}
    void foo(Iterator<Object> x) {}
    void foo(Set<Object> x) {}
    void foo(Map<Object, Object> x) {}
    void foo(Map.Entry<Object, Object> x) {}

    void foo1(List<List<Object>> x) {}
}

// FILE: main.kt

fun main(
        a: A,
        ml: MutableList<String>, l: List<String>,
        ms: MutableSet<String>, s: Set<String>,
        mm: MutableMap<Any, String>, m: Map<Any, String>,
        mme: MutableMap.MutableEntry<Any, String>, me: Map.Entry<Any, String>,
        mll: MutableList<MutableList<String>>, ll: List<List<String>>
) {
    // Lists
    a.foo(ml)
    a.foo(l)
    a.foo(ml as MutableList<Any>)
    a.foo(l as List<Any>)

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
    a.foo(ms as MutableSet<Any>)
    a.foo(s as Set<Any>)

    // Maps
    a.foo(mm)
    a.foo(m)
    a.foo(mm as MutableMap<Any, Any>)
    a.foo(m as Map<Any, Any>)

    // Map entries
    a.foo(mme)
    a.foo(me)
    a.foo(mme as MutableMap.MutableEntry<Any, Any>)
    a.foo(me as Map.Entry<Any, Any>)

    // Lists of lists
    a.foo1(mll)
    a.foo1(ll)
    a.foo1(mll as MutableList<MutableList<Any>>)
    a.foo1(ll as List<List<Any>>)

}
