// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: 1.kt

package test

open class Base(val name: String)
class A(name: String) : Base(name)
class B(name: String) : Base(name)

var result = "fail"

fun foo(base: Array<out Base>) {
    result = base[0].name
}

fun cond() = true

inline fun <reified T : Base, reified Y : Base> process(a: Base) {
    val z = try {
        arrayOf<T>(a as T)
    } catch (e: Exception) {
        arrayOf<Y>(a as Y)
    }

    foo(z)
}


// FILE: 2.kt
import test.*

fun box(): String {
    process<A, B>(B("OK"))

    return result
}
