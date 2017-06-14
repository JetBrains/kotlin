// "Remove redundant spread operator" "true"

fun foo(vararg x: Char) {}

fun bar() {
    foo(*charArrayOf<caret>('a', 'b'))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection