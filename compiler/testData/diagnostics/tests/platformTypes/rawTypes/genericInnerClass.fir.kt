// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

class A<T> {

    void foo(T x) {}

    public class Inner<E> {
        Inner(E x0, T x, List<T> y) {}

        void foo(E x0, T x, List<T> y) {}
        A<Map<E, T>> bar() {}
    }
}

// FILE: Test.java

class Test {
    static A rawAField = null;
}

// FILE: main.kt

val strList: List<String> = null!!

fun main() {
    val rawA = Test.rawAField
    var rawInner = rawA.Inner<Double>(<!ARGUMENT_TYPE_MISMATCH!>""<!>, "", strList)
    rawInner.foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>, "", strList)
    rawInner.bar().foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}

