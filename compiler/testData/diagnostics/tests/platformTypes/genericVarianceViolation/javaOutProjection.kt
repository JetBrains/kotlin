// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;


public class A {
    void foo(List<Object> x) {}
    <T> void foo2(List<T> x) {}
    List<? extends Number> bar() {}
}
// FILE: main.kt

fun main(a: A) {
    a.foo(<!JAVA_TYPE_MISMATCH!>a.bar()<!>)
    a.foo2(a.bar())
}
