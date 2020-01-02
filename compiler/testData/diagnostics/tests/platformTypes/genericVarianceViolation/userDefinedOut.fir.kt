// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(Out<Object> x) {}
    void bar(Out<? extends Object> x) {}
}
// FILE: main.kt

public class Out<out T>()

fun main(a: A, o: Out<String>) {
    a.foo(o)
    a.bar(o)
}
