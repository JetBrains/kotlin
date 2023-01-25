// FILE: main.kt
package a.b.c

import dependency.foo

fun foo() {}

open class Base {
    companion object {
        fun foo() {}
    }
}

class Child : Base() {
    fun test() {
        <expr>Base.foo()</expr>
    }
}

// FILE: dep.kt
package dependency

fun foo() {}