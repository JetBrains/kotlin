// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71708

inline fun build(action: () -> Unit) {}

fun foo(x: Int) = build {
    if (x == 1) <!UNSUPPORTED!>[1]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, equalityExpression, functionDeclaration, functionalType, ifExpression, inline,
integerLiteral, lambdaLiteral */
