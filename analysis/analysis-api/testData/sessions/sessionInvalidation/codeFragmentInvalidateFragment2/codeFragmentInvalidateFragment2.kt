// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt

// MODULE: sourceModule(library)
// MODULE_KIND: Source
// FILE: context.kt
fun test() {
    <caret_context>Unit
}

val a: Int = 0

// MODULE: fragment1.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: sourceModule

// FILE: fragment1.kt
// CODE_FRAGMENT_KIND: BLOCK
fun bar() {
    val b: Int = 5
    <caret_context>Unit
}

// MODULE: fragment2.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: fragment1.kt
// WILDCARD_MODIFICATION_EVENT

// FILE: fragment2.kt
// CODE_FRAGMENT_KIND: EXPRESSION
a <caret>+ b
