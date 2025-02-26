// DUMP_CODE

// MODULE: context
// FILE: context.kt

inline fun <reified T> foo() {
    <caret_context>val x = 1
}

fun main() {
    <caret_stack_0>foo<String>()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
T::class.simpleName