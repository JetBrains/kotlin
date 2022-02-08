// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt
@file:JvmName("TTest")
@file:JvmMultifileClass
package test

var property = "fail"
    private set

fun test() {
    property = "OK"
}

// MODULE: main(lib)
// FILE: B.kt

import test.*

fun box(): String {
    test()
    return property
}
