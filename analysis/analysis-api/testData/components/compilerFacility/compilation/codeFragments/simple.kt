// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
2 + 2 * 2