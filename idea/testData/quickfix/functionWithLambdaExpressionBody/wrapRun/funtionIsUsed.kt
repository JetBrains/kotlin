// "Convert to run { ... }" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.FunctionWithLambdaExpressionBodyInspection
// ACTION: Convert to block body
// ACTION: Convert to multi-line lambda
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Specify explicit lambda signature
// ACTION: Specify explicit lambda signature
// ACTION: Specify return type explicitly
fun test(a: Int, b: Int) = <caret>{ a + b }

val foo = test(1, 2)()