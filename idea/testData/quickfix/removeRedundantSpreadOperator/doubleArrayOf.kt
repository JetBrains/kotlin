// "Remove redundant spread operator" "true"

fun foo(vararg x: Double) {}

fun bar() {
    foo(*doubleArrayOf<caret>(1.0))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection