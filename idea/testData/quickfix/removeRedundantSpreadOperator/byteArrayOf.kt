// "Remove redundant spread operator" "true"

fun foo(vararg x: Byte) {}

fun bar() {
    foo(*byteArrayOf<caret>(1))
}

// TOOL: org.jetbrains.kotlin.idea.inspections.RemoveRedundantSpreadOperatorInspection