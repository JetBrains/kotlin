// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: J.java

public class J {
    public final String result;

    public J(String result) {
        this.result = result;
    }
}

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
