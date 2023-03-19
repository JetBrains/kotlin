// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: 1.kt

package test

enum class Base(val value: String) {
    OK("OK"),
    B("FAIL");
}

enum class Base2(val value: String) {
    A("OK2"),
    B("FAIL2");
}

var result = "fail"

fun foo(base: Enum<*>) {
    result = base.name
}

fun foo(base: Array<out Enum<*>>) {
    result = base[0].name
}

fun cond() = true

inline fun <reified T : Enum<T>, reified Y : Enum<Y>> process(a: String) {
    val z = try {
        enumValues<T>()
    } catch (e: Exception) {
        enumValues<Y>()
    }

    foo(z)
}

// FILE: 2.kt
import test.*

fun box(): String {
    process<Base, Base2>("OK")

    return result
}
