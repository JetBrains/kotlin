// IGNORE_BACKEND_FIR: JVM_IR
// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// WITH_REFLECT
// MODULE: lib
// FILE: J.java

public class J {
    public final String result;

    public J(String result) {
        this.result = result;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun box(): String {
    val reference = ::J
    val javaConstructor = reference.javaConstructor ?: return "Fail: no Constructor for reference"
    val j = javaConstructor.newInstance("OK")
    val kotlinConstructor = javaConstructor.kotlinFunction
    if (reference != kotlinConstructor) return "Fail: reference != kotlinConstructor"
    return j.result
}
