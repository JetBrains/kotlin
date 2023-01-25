// FILE: main.kt
package a.b.c

import dependency.foo

fun foo() {}

class Outer {
    class Inner {
        fun test() {
            <expr>Outer.Inner.foo()</expr>
        }
        companion object {
            fun foo() {}
        }
    }
}

// FILE: dep.kt
package dependency

fun foo() {}