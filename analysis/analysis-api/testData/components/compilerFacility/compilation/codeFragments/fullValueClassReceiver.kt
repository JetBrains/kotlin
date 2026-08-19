// LANGUAGE: +FullValueClasses

// MODULE: context
// FILE: context.kt
value class Point(val x: Int, val y: Int) {
    fun test() {
        <caret_context>Unit
    }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Point(x + 1, y + 1)
