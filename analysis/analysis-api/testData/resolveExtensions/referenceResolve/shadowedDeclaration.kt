// UNRESOLVED_REFERENCE

// FILE: declarations.hidden.kt
package foo

fun bar() = "baz"

// FILE: main.kt
fun main() {
    val x = foo.<caret>bar()
}