// FILE: 1.kt
class E<T>(val x: T) {
    inner class Inner {
        inline fun foo(): T = x
    }
}

// FILE: 2.kt

inline class IC(val s: String)

fun box(): String = E(IC("OK")).Inner().foo().s
