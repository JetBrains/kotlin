// "Remove redundant spread operator" "true"

fun foo(vararg x: Float) {}

fun bar() {
    foo(*floatArrayOf<caret>(1.0f))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection