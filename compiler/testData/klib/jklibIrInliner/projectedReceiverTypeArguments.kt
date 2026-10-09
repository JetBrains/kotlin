// MODULE: lib
// FILE: lib.kt
package lib

open class Box<T>(val value: T) {
    inline fun get(): T {
        val v: T = value
        return v
    }
}

open class SubBox<U>(value: U) : Box<U>(value)

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.Box
import lib.SubBox

// `T` is only known to be a supertype of `String`, so it is erased.
fun testIn(box: Box<in String>): Any? = box.get()

fun testInInherited(box: SubBox<in String>): Any? = box.get()

// Every value is a `Number`, so `T` is substituted with `Number`.
fun testOut(box: Box<out Number>): Number = box.get()

fun testOutInherited(box: SubBox<out Number>): Number = box.get()
