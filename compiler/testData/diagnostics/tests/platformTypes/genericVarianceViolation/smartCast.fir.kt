// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
}
// FILE: main.kt

fun main(a: A, ml: Any) {
    if (ml is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!>) {
        a.foo(<!JAVA_TYPE_MISMATCH!>ml<!>)
        a.foo(ml as List<Any>)
    }
}
