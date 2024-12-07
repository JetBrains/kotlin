// MODULE: sourceModule
// MODULE_KIND: Source
// FILE: context.kt
fun test() {
    <caret_context>Unit
}

val a: Int = 0
val b: Int = 5
val c: Int = 10

// MODULE: fragment1.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: sourceModule
// WILDCARD_MODIFICATION_EVENT

// FILE: fragment1.kt
// CODE_FRAGMENT_KIND: EXPRESSION
a <caret>+ b

// MODULE: fragment2.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: sourceModule

// FILE: fragment2.kt
// CODE_FRAGMENT_KIND: EXPRESSION
a <caret>+ c
