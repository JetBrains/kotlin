// FILE: main.kt
package test

import dependency.Outer.NestedSAM

val handler = NestedSAM {}

// FILE: dependency.kt
package dependency

class Outer {
    fun interface NestedSAM {
        fun method()
    }
}