// FULL_JDK
// TARGET_BACKEND: JVM
// IGNORE_DEXING
//  ^ 2.1.75 fails with AssertionError (2.0.88 doesn't). TODO: investigate in more detail.
// FILE: 1.kt

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
