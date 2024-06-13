// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;


public class A {
    void foo(List<Object> x) {}
    <T> void foo2(List<T> x) {}
    void foo3(Collection<? extends Object> x) {}
}

// FILE: B.java

import java.util.*;

public class B<T> {
    public B(Collection<T> c) {

    }
}
// FILE: main.kt

fun main(a: A) {
    a.foo(<!JAVA_TYPE_MISMATCH!>bar()<!>)
    a.foo2(bar())
    a.foo3(bar())
    B(bar())
    a.foo(<!JAVA_TYPE_MISMATCH!>bar2()<!>)
    a.foo2(bar2())
    a.foo3(bar2())
    B(bar2())
}

fun bar() : MutableList<out Number> {
    TODO()
}

fun bar2() : MutableList<MutableList<out Number>> {
    TODO()
}
