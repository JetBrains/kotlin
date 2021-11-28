// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/D.java

package test;

public class D {
    protected String field = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

import test.D

fun <T> eval(fn: () -> T) = fn()

class A : D() {
    fun a(): String {
        return eval { field!! }
    }
}

fun box(): String {
    return A().a()
}
