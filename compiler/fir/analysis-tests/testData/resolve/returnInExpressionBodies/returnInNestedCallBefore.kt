// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases, +ContextParameters
// DIAGNOSTICS: -REDUNDANT_RETURN
fun foo(s: String): String = s

fun test1(a: String?) = foo(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")
fun test2(a: String?): String = foo(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")

fun test3(a: String?) = foo(s = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")
fun test4(a: String?): String = foo(s = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")


fun test5(a: String?) = foo((fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")())
fun test6(a: String?): String = foo((fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")())
fun test7(a: String?) = foo((fun(): String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "")())

fun String.funWithExt(): String = this
fun test8(a: String?) = (a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "").funWithExt()
fun test9(a: String?): String = (a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "").funWithExt()

context(a: String)
fun funWithContext(): String = a
fun test10(a: String?) = with(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "") { funWithContext() }
fun test11(a: String?): String = with(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "") { funWithContext() }

/* GENERATED_FIR_TAGS: anonymousFunction, elvisExpression, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, lambdaLiteral, nullableType, stringLiteral, thisExpression */
