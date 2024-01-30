// MODULE: context

// FILE: context.kt
fun test() {
    val x = 0
    <caret_context>val y = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
x