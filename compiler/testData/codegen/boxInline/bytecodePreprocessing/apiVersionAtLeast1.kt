// TARGET_BACKEND: JVM

// Wrong function resolution after package renaming
// IGNORE_BACKEND: ANDROID

// FILE: 1.kt
package kotlin.internal

fun apiVersionIsAtLeast(epic: Int, major: Int, minor: Int): Boolean {
    return false
}

var properFunctionWasClled = false

fun doSomethingNew() {
    properFunctionWasClled = true
}

fun doSomethingOld() {
    throw AssertionError("Should not be called")
}

inline fun versionDependentInlineFun() {
    if (apiVersionIsAtLeast(1, 1, 0)) {
        doSomethingNew()
    }
    else {
        doSomethingOld()
    }
}

fun test() {
    versionDependentInlineFun()
}


// FILE: 2.kt
import kotlin.internal.*

fun box(): String {
    test()
    if (!properFunctionWasClled) return "Fail 1"

    return "OK"
}
