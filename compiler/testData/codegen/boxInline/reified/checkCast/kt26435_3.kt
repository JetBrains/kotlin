// FILE: 1.kt
// WITH_RUNTIME

package test

open class Base(val name: String)
class A(name: String) : Base(name)
class B(name: String) : Base(name)

var result = "fail"

fun foo(base: Base) {
    result = base.name
}

fun cond() = true

inline fun <reified T: Base, reified Y : Base> process(a: Base): Base {
    val z = if (cond())
        a as T
    else
        a as Y
    return z
}


// FILE: 2.kt
import test.*

fun box(): String {
    return process<A, B>(A("OK")).name
}