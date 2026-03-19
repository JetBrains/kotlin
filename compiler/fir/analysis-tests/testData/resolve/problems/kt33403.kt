// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-33403
// WITH_STDLIB

// KT-33403: Diagnostic about useless cast

fun test() {
    val a = 42 <!USELESS_CAST!>as Int<!>
    val b = "hello" <!USELESS_CAST!>as String<!>
    println(a)
    println(b)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
stringLiteral */
