// MODULE: context

// FILE: context.kt
fun foo() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
val a = 5
a * 2