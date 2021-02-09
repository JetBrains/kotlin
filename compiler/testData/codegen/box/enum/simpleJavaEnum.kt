// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/En.java

package test;

public enum En {
    A;
}

// MODULE: main(lib)
// FILE: 1.kt

import test.*

fun box() =
    if (En.A.toString() == "A") "OK"
    else "fail"
