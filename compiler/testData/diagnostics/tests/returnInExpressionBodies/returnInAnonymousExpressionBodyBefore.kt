// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases, +ContextParameters

fun test1() = fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1

fun test2(): () -> Int = fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1

fun test3() = fun(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1

fun test4() = fun() { return }

fun test5() = fun Int.() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> this

fun test6() = fun Int.(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> this

fun test7() = context(a: Int) fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> a

fun test8() = context(a: Int) fun(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> a

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, thisExpression */
