// FULL_JDK
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test

import java.util.concurrent.Callable

fun test(): String  = ""

inline fun String.switchMapOnce(crossinline mapper: (String) -> String): String {
    Callable(::test)
    return { mapper(this) }.let { it() }
}
// FILE: 2.kt

import test.*

fun box() : String {
    return "O".switchMapOnce {

        "K".switchMapOnce {
            "OK"
        }
    }
}
