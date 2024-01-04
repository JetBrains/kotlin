// FILE: main.kt
package test

val handler = <expr>dependency.Outer.NestedSAM {}</expr>

// FILE: dependency.kt
package dependency

class Outer {
    fun interface NestedSAM {
        fun method()
    }
}
