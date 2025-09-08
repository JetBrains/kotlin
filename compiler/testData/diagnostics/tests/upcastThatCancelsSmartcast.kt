// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-80222

fun Number.foo() = 1
fun Int.foo() = 2

fun test(a: Number): Int {
    if (a !is Int) return 0

    return (a as Number).foo()
}

/* GENERATED_FIR_TAGS: asExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral,
isExpression, smartcast */
