// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: test.kt

fun test2() {
}

// FILE: main.kt
// See KT-10690 Exception in kotlin.reflect when trying to get kotlinFunction from javaMethod

import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

fun box(): String {
    if (::box.javaMethod?.kotlinFunction == null)
        return "Fail box"
    if (::test1.javaMethod?.kotlinFunction == null)
        return "Fail test1"
    if (::test2.javaMethod?.kotlinFunction == null)
        return "Fail test2"

    return "OK"
}

fun test1() {
}
