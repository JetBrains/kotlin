// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType, +ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases

inline fun <T> runInline(block: (T) -> T): T = block(null!!)

fun test1() = runInline<Int> { <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> 1 }

fun test2(): Int = runInline<Int> { return 1 }

fun test3() = runInline<Int> { a: Int -> <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> a }

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral,
nullableType, typeParameter */
