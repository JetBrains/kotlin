// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
    List<String> bar() {}
}
// FILE: main.kt

fun main(a: A) {
    a.foo(a.bar())
}
