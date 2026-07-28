// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87881
// WITH_STDLIB

fun Number.f() {}

fun foo(bar: String = "20") = buildList {
    add(30)
    this.f() // `UNRESOLVED_REFERENCE_WRONG_RECEIVER` here prevents the fixation of `TypeVariable(K)` of `if (true) 10 else "20"`.

    if (bar == if (true) 10 else "20") {
        bar is Int || bar is String
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, smartcast, whenExpression */
