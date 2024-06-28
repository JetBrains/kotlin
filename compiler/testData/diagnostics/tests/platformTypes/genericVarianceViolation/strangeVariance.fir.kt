// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<? super String> x) {}
    void bar(List<? super Object> x) {}
}
// FILE: main.kt

fun main(a: A, ml: MutableList<String>, l: List<String>) {
    a.foo(ml)
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>l<!>)

    a.bar(<!ARGUMENT_TYPE_MISMATCH!>ml<!>)
    a.bar(<!ARGUMENT_TYPE_MISMATCH!>l<!>)
}
