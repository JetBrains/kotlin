// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface I1
interface I2

fun foo(i: I1) {}
fun foo(i: I2) {}

fun bar(i: I1) {
    if (i is I2) {
        foo(i <!USELESS_CAST!>as I1<!>)
        foo(i as I2)
    }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, smartcast */
