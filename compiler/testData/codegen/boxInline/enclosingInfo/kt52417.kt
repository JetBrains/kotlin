// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_STDLIB
// FILE: 1.kt
package test

abstract class TypeToken<U>

// Although V is not reified, if the object happens to be regenerated, V will be replaced with its value in signatures
inline fun <V> typeTokenOf(crossinline forceRegeneration: () -> Unit = {}) =
    object : TypeToken<V>() {
        fun unused() = forceRegeneration()
    }

// FILE: 2.kt
import test.*

fun interface I {
    fun foo(): String
}

fun <T> foo() =
    I {
        typeTokenOf<T>()::class.java.genericSuperclass.toString()
    }.foo()

fun box(): String =
    foo<String>().let { if (it == "test.TypeToken<T>") "OK" else it }
