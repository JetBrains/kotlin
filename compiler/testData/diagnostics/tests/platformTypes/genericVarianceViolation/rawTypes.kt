// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List x) {}
}
// FILE: main.kt

fun main(a: A, ml: MutableList<String>, l: List<String>) {
    a.foo(ml)
    a.foo(l)
}
