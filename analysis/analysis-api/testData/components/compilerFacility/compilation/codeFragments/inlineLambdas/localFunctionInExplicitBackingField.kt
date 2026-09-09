// LANGUAGE: +ExplicitBackingFields
// DUMP_CODE

// MODULE: context
// FILE: context.kt

class Foo {
    val prop: Number
        field: Int = run {
            fun call(a: Int): Int = a + 1

            val x = 2
            <caret_stack_0>bar { call(x) }
        }
}

inline fun bar(block: () -> Int): Int {
    <caret_context>return block()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block()
