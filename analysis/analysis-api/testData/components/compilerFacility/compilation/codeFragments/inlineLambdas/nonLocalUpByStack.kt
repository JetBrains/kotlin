// DUMP_CODE

// MODULE: context
// FILE: context.kt

fun main() {
    <caret_stack_2>foo1 { return }
}

inline fun foo1(block: () -> Unit) {
    <caret_stack_1>foo2 { block() }
}

inline fun foo2(block: () -> Unit) {
    <caret_stack_0>foo3 { block() }
}

inline fun foo3(block: () -> Unit) {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block()