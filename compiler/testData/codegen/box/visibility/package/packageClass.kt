// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: protectedPack/J.java

package protectedPack;

class J {
    public String test() {
        return "OK";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

package protectedPack

fun box(): String {
    return J().test()!!
}
