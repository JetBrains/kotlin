// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87881
// WITH_STDLIB
// DUMP_CFG: FLOW
// RENDER_DIAGNOSTICS_FULL_TEXT

fun Number.f() {}

fun foo(bar: String = "20") = buildList {
    add(30)
    <!ARGUMENT_TYPE_MISMATCH!>this<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>() // `UNRESOLVED_REFERENCE_WRONG_RECEIVER` here prevents the fixation of `TypeVariable(K)` of `if (true) 10 else "20"`.

    <!ARGUMENT_TYPE_MISMATCH, SMARTCAST_TO_TYPE_VARIABLE!>bar<!> == <!ARGUMENT_TYPE_MISMATCH!>if (true) <!ARGUMENT_TYPE_MISMATCH!>10<!> else <!ARGUMENT_TYPE_MISMATCH!>"20"<!><!>
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, smartcast, whenExpression */
