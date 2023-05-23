// UNRESOLVED_REFERENCE

// MODULE: extendedModule
// FILE: declarations.hidden.kt
package foo

fun bar() = "baz"

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
// FILE: main.kt
fun main() {
    val x = foo.<caret>bar()
}