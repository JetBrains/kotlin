// IGNORE_BACKEND: NATIVE
// FILE: A.kt
@file:JvmName("TTest")
@file:JvmMultifileClass
package test

var property = "fail"
    private set

fun test() {
    property = "OK"
}

// FILE: B.kt

import test.*

fun box(): String {
    test()
    return property
}
