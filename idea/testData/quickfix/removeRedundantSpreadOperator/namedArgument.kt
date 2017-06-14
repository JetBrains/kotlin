// "Remove redundant spread operator" "true"

fun foo(vararg x: String) {}

fun bar() {
    foo(x = *arrayOf<caret>("abc"))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection