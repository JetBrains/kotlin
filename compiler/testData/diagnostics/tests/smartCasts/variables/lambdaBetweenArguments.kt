// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int, f: () -> Unit, y: Int) {}

fun bar() {
    var x: Int?
    x = 4
    foo(<!DEBUG_INFO_SMARTCAST!>x<!>, { x = null; x<!UNSAFE_CALL!>.<!>hashCode() }, <!SMARTCAST_IMPOSSIBLE!>x<!>)
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, smartcast */
