// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
}
// FILE: main.kt

fun main(a: A, ml: Any) {
    if (ml is MutableList<String>) {
        a.foo(ml)
        a.foo(ml as List<Any>)
    }
}
