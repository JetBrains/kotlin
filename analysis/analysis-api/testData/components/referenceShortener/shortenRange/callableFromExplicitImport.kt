// FILE: main.kt
package a.b.c

fun foo() {}

fun test() {
    <expr>dependency.foo()</expr>
}

// FILE: dependency.kt
package dependency

fun foo() {}