// LANGUAGE: +ForbidParenthesizedLhsInAssignments

// MODULE: context

// FILE: context.kt
class Test {
    var x: Int = 0

    fun test() {
        <caret_context>val y = 0
    }
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
@Suppress("UNUSED_VARIABLE") x = 1