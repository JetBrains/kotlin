// "Remove redundant spread operator" "true"

fun foo(vararg x: String) {}

fun bar() {
    foo(*emptyArray<caret><String>())
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection