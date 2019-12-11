// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
}
// FILE: main.kt

abstract class B : MutableList<String>

fun main(a: A, b: B) {
    a.foo(b)
    a.foo(b as List<Any>)
}
