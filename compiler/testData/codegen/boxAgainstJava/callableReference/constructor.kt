// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: A.java

class A {
    public A(double x, int y) { }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    (::A)(0.0, 0)
    return "OK"
}
