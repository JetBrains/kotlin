// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
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
