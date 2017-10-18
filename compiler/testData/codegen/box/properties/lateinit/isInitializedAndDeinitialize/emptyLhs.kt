// WITH_RUNTIME

package test

import kotlin.reflect.KProperty
import test.Derived.p

class Foo {
    lateinit var p: String

    fun test(): Boolean {
        if (!::p.isInitialized) {
            p = "OK"
            return false
        }
        return true
    }
}

object Bar {
    lateinit var p: String

    fun test(): Boolean {
        if (!::p.isInitialized) {
            p = "OK"
            return false
        }
        return true
    }
}

open class Base(val b: Boolean)

object Derived : Base(::p.isInitialized) {
    lateinit var p: String
}

fun box(): String {
    val foo = Foo()
    if (foo.test()) return "Fail 1"
    if (!foo.test()) return "Fail 2"

    val bar = Bar
    if (bar.test()) return "Fail 3"
    if (!bar.test()) return "Fail 4"

    if (Derived.b) return "Fail 5"

    return bar.p
}
