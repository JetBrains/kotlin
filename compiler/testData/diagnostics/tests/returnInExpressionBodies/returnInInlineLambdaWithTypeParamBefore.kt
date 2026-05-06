// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases

inline fun <T> runInline(block: (T) -> T): T = block(null!!)

fun test1() = runInline<Int> { return 1 }

fun test2(): Int = runInline<Int> { return 1 }

fun test3() = runInline<Int> { a: Int -> return a }

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral,
nullableType, typeParameter */
