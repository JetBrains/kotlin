// UNRESOLVED_REFERENCE

// MODULE: extendedModule
// WITH_RESOLVE_EXTENSION
// RESOLVE_EXTENSION_PACKAGE: generated
// RESOLVE_EXTENSION_SHADOWED: \.hidden\.kt$

// FILE: declarations.hidden.kt
package foo

fun bar() = "baz"

// MODULE: dependency2

// MODULE: main(extendedModule, dependency2)()()
// FILE: main.kt
fun main() {
    val x = foo.<caret>bar()
}