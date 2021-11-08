// FILE: foo/A.kt
package foo

interface A {
    fun fest() {}
}

// FILE: bar/A.kt
package bar

interface A {
    fun rest() {}
}

// FILE: main.kt
import foo.*
import bar.*

val a = A? = null // AMBIGUITY here

fun test() {
    a?.fest()
    a?.rest()
}
