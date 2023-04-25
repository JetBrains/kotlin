// FILE: main.kt
package a.b.c

class A {
    fun foo() {}
}
fun foo() = 1

context(A)
fun test() {
    <expr>a.b.c.foo()</expr>
}
