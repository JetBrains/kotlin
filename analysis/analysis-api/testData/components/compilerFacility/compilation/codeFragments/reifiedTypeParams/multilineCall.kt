// DUMP_CODE

// MODULE: context
// FILE: context.kt

inline fun <reified T> bar(x: Int, y: Int): Int {
    <caret_context>return 1
}

fun main() {
    <caret_stack_0>bar<Int>(bar<String>(
            1,
            2
        ),
        bar<Any>(
            3,
            4
        )
    )
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
T::class.qualifiedName