// FILE: main.kt
package a.b.c

fun <T, E, D> foo(a: T, b: E, c: D) = a.hashCode() + b.hashCode() + c.hashCode()

fun <E> E.foo() = hashCode()

object Receiver {
    fun <E> E.foo(x: E, y: E, z: E) = x.hashCode() + y.hashCode() + z.hashCode()

    fun foo(a: Int, b: Boolean, c: String) = a.hashCode() + b.hashCode() + c.hashCode()

    fun test(): Int {
        fun foo(a: Int, b: Boolean, c: String) = a + b.hashCode() + c.hashCode()
        return <expr>Receiver.foo(1, false, "bar")</expr>
    }
}
