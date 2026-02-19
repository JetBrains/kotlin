// IGNORE_FE10
// FE10 expects tests to have at least one source module.

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: foo.kt
fun foo(a: Int, b: String) {}

// MODULE: main
// MODULE_KIND: LibrarySource
// FALLBACK_DEPENDENCIES
// FILE: main.kt
fun call() {
    <expr>foo(1, "foo")</expr>
}
