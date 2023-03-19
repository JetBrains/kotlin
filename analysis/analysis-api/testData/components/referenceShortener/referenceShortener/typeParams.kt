// FILE: main.kt
package a.b.c

fun <T> foo(a: T, b: T) = a.hashCode() + b.hashCode()

fun test(): Int {
    fun foo(a: Int, b: Int) = a + b
    return <expr>a.b.c.foo(1, 2)</expr>
}