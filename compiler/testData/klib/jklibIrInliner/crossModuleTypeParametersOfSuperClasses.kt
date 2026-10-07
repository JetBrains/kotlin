// MODULE: lib
// FILE: lib.kt
package lib

open class Base<T>(val value: T) {
    inline fun get(): T {
        val v: T = value
        return v
    }
}

class Sub<U>(value: U) : Base<U>(value)

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.Sub

// `T` is substituted with `String`, inherited through the type argument of `Sub`.
fun test(sub: Sub<String>): String = sub.get()

// The type argument of `U`, and so of `T`, is unknown, so `T` is erased instead of being replaced with `U`, which is out of scope here.
fun testStar(sub: Sub<*>): Any? = sub.get()
