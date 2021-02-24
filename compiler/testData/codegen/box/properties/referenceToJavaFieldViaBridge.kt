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

class A : D() {
    fun a(): String {
        return {field!!}()
    }
}

fun box(): String {
    return A().a()
}
