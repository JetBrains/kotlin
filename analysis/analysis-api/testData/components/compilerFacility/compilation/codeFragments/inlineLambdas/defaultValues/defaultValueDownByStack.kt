// DUMP_CODE

// MODULE: context
// FILE: context.kt

inline fun foo3(block: (Int) -> Int) {
    <caret_context>val x = 1
}

inline fun foo2(block: (Int) -> Int) {
    <caret_stack_0>foo3(block)
}

inline fun foo1(block: (Int) -> Int = { it + 1 }) {
    <caret_stack_1>foo2(block)
}

fun main() {
    <caret_stack_2>foo1()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block(42)