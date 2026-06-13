// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-645

// KT-645: "Overload resolution ambiguity" for integer literal and nullable parameter type
fun foo(other: Int?) {}
fun foo(other: Short?) {}

fun test1() {
    foo(11)
    foo(11 <!INTEGER_LITERAL_CAST_INSTEAD_OF_TO_CALL!>as Int<!>)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral, nullableType */
