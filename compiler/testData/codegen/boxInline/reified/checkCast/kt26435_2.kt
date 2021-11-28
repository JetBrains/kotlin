// WITH_STDLIB
// FILE: 1.kt

package test

open class Base(val name: String)
class A(name: String) : Base(name)
class B(name: String) : Base(name)

var result = "fail"

fun foo(base: Base) {
    result = base.name
}

fun cond() = true

inline fun <reified T : Base, reified Y : Base> process(a: Base) {
    val z = if (cond())
        a as T
    else
        a as Y
    foo(z)
}


// FILE: 2.kt
import test.*

fun box(): String {
    process<A, B>(A("OK"))

    return result
}
