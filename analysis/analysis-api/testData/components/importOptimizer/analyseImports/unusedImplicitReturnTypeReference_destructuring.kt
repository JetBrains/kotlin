// FILE: main.kt
package test

import dependency.foo
import dependency.Bar
import dependency.One
import dependency.Two

fun usage() {
    val (one, two) = foo()
}

// FILE: dependency.kt
package dependency

fun foo(): Bar = Bar()

data class Bar(val one: One, val two: Two)

class One
class Two
