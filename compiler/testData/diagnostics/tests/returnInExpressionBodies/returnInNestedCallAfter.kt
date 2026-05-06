// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType, +ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases, +ContextParameters
// DIAGNOSTICS: -REDUNDANT_RETURN
fun foo(s: String): String = s

fun test1(a: String?) = foo(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "")
fun test2(a: String?): String = foo(a ?: return "")

fun test3(a: String?) = foo(s = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "")
fun test4(a: String?): String = foo(s = return "")


fun test5(a: String?) = foo((fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "")())
fun test6(a: String?): String = foo((fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "")())
fun test7(a: String?) = foo((fun(): String = return "")())

fun String.funWithExt(): String = this
fun test8(a: String?) = (a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "").funWithExt()
fun test9(a: String?): String = (a ?: return "").funWithExt()

context(a: String)
fun funWithContext(): String = a
fun test10(a: String?) = with(a ?: <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> "") { funWithContext() }
fun test11(a: String?): String = with(a ?: return "") { funWithContext() }

/* GENERATED_FIR_TAGS: anonymousFunction, elvisExpression, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, lambdaLiteral, nullableType, stringLiteral, thisExpression */
