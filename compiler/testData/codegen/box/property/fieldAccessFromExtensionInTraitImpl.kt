// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: D.java

public class D {
    public final String result = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

// KT-4878

interface T {
    fun Int.foo(d: D) = d.result!!
}

class A : T {
    fun bar() = 42.foo(D())
}

fun box() = A().bar()
