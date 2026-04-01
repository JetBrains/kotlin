// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType, +ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases, +ContextParameters

fun test1() = fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> 1

fun test2(): () -> Int = fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> 1

fun test3() = fun(): Int = <!REDUNDANT_RETURN!>return<!> 1

fun test4() = fun() { return }

fun test5() = fun Int.() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> this

fun test6() = fun Int.(): Int = <!REDUNDANT_RETURN!>return<!> this

fun test7() = context(a: Int) fun() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> a

fun test8() = context(a: Int) fun(): Int = <!REDUNDANT_RETURN!>return<!> a

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, thisExpression */
