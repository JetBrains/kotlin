// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated
// RESOLVE_EXTENSION_SHADOWED: \.hidden\.kt$
// UNRESOLVED_REFERENCE

// FILE: declarations.hidden.kt
package foo

fun bar() = "baz"

// FILE: main.kt
fun main() {
    val x = foo.<caret>bar()
}