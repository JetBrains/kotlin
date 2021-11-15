// TARGET_BACKEND: JVM
// WITH_STDLIB
//FULL_JDK
// FILE: 1.kt
package test

inline fun crashMe(crossinline callback: () -> Unit): Function0<Unit> {
    return object: Function0<Unit> {
        override fun invoke() {
            callback()
        }
    }
}

// FILE: 2.kt

import test.*
import java.lang.reflect.Modifier

var result = "fail"


fun box(): String {
    val crashMe = crashMe { result = "OK" }
    val modifiers = crashMe::class.java.getDeclaredConstructor().modifiers
    if (!Modifier.isPublic(modifiers)) return "fail $modifiers"

    crashMe.invoke()
    return result
}
