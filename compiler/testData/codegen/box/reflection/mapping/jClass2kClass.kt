// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// WITH_RUNTIME
// MODULE: lib
// FILE: J.java

public class J {}

// MODULE: main(lib)
// FILE: 1.kt

import kotlin.test.assertEquals

fun box(): String {
    val j = J::class.java
    assertEquals(j, j.kotlin.java)

    return "OK"
}
