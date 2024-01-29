// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

val foo = "foo"


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret>foo