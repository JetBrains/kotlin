// FILE: main.kt
package a.b.c

class MyClass(val id: Int) {
    companion object {
        fun foo() = ""
    }
}

fun test() {
    <expr>a.b.c.MyClass.Companion.foo()</expr>
}

fun foo() = ""