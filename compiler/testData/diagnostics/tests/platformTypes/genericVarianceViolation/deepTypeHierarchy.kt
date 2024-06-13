// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;


public class A {
    void foo(List<Object> x) {}
    <T> void foo2(List<T> x) {}
}
// FILE: main.kt

fun main(a: A) {
    a.foo(<!JAVA_TYPE_MISMATCH!>bar()<!>)
    a.foo2(bar())
}

interface MyList1<T>: MutableList<T>
interface MyList2<A, B>: MyList1<B>
interface MyList3<A2, B> : MyList2<Any, B>

fun bar(): MyList3<Int, out String> {
    TODO()
}
