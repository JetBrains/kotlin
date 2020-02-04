// "Remove braces" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.FunctionWithLambdaExpressionBodyInspection
// ACTION: Convert to block body
// ACTION: Convert to run { ... }
// ACTION: Specify explicit lambda signature
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Specify explicit lambda signature
// ACTION: Specify return type explicitly
fun test(a: Int, b: Int) = <caret>{
    foo()
    foo()
}

fun foo() {}