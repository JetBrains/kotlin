// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: A.java

public class A {
    public final String field = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() = (A::field).get(A())
