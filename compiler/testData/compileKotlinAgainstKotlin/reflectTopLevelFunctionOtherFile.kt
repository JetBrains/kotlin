// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: A.kt

fun test() {
}

// FILE: B.kt

import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

fun box(): String {
    val r = Class.forName("AKt").methods.single { it.name == "test" }.kotlinFunction
    if (r?.toString() != "fun test(): kotlin.Unit")
        return "Fail: $r"

    return "OK"
}
