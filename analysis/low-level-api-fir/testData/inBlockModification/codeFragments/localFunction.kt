// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
fun foo(a: Int, b: String) {}

fun bar() {
    <expr>foo(1, "foo")</expr>
}

bar()