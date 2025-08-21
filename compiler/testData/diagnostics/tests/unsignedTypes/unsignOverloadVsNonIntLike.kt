// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70447

fun foo(n: UByte) {}
fun foo(n: String) {}

fun bar(n: Long) {}
fun bar(n: String) {}

fun main() {
    val a = ""
    a <!CAST_NEVER_SUCCEEDS!>as<!> UByte
    foo(a)

    val b = ""
    b <!CAST_NEVER_SUCCEEDS!>as<!> Long
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(b)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, intersectionType, localProperty, propertyDeclaration,
smartcast, stringLiteral */
