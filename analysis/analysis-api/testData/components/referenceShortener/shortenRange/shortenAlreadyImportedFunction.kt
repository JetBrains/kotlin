// FILE: main.kt
package a.b.c

import dependency.foo

fun foo() {}

class Outer {
    fun foo() {}
    class Inner {
        fun test() {
            <expr>a.b.c.foo()</expr>
        }
    }
}

// FILE: dep.kt
package dependency

fun foo() {}