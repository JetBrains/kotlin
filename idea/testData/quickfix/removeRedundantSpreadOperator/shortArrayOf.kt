// "Remove redundant spread operator" "true"

fun foo(vararg x: Short) {}

fun bar() {
    foo(*shortArrayOf<caret>(1))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection