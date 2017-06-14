// "Remove redundant spread operator" "true"

fun foo(vararg x: String) {}

fun bar() {
    foo(*arrayOf<caret>("abc", "def"), "ghi", "jkl")
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection