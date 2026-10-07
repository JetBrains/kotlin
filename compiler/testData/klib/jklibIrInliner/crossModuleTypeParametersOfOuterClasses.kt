// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// MODULE: lib
// FILE: lib.kt
package lib

class Box<T>(val value: T) {
    inline fun get(): T {
        val v: T = value
        return v
    }
}

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.Box

// `T` is substituted with `String`.
fun test(box: Box<String>): String = box.get()

// The type argument of `T` is unknown, so `T` is erased.
fun testStar(box: Box<*>): Any? = box.get()
