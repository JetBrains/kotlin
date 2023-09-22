// FILE: main.kt
package test

fun usage() {
    <expr>dependency</expr>.foo()
}

// FILE: dependency.kt
package dependency

val foo: Int = 10
