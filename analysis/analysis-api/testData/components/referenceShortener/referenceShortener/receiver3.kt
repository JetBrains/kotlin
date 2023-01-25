// IGNORE_FIR
// FILE: main.kt
package a.b.c

fun <T, E, D> foo(a: T, b: E, c: D) = a.hashCode() + b.hashCode() + c.hashCode()

fun <E> E.foo() = hashCode()

object Receiver {
    fun <T, E, D> foo(a: T, b: E, c: D) = a.hashCode() + b.hashCode() + c.hashCode()

    fun foo(a: Int, b: Boolean, c: String) = a.hashCode() + b.hashCode() + c.hashCode()

    fun test(): Int {
        fun foo(a: Int, b: Boolean, c: Int) = a + b.hashCode() + c
        return <expr>Receiver.foo(1, false, "bar")</expr>
    }
}