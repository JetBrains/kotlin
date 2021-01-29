// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// FILE: 1.kt
inline fun cross(crossinline fn: () -> String) : Any =
    object {
        override fun toString(): String = fn()
    }

fun foo(fn: () -> String) = fn()

// FILE: 2.kt
fun box() =
    cross {
        foo { "OK" }
    }.toString()