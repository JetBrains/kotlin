// DUMP_CODE

// MODULE: context
// FILE: context.kt

inline fun foo(block: (Int) -> Int = { it + 1 }) {
    <caret_context>val x = 1
}

fun main() {
    <caret_stack_0>foo { -1 }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block(42)