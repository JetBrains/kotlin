// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: A.java

class A {
    public A() {}

    public A(String x) {}

    public A(long l, double z) {}
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    A()
    A("")
    A(0.toLong(), 0.0)
    return "OK"
}
