// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// FULL_JDK

package test

import java.util.concurrent.Callable

fun test(): String  = ""

inline fun String.switchMapOnce(crossinline mapper: (String) -> String): String {
    Callable(::test)
    return { mapper(this) }()
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

