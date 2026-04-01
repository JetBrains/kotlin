// RUN_PIPELINE_TILL: FRONTEND
val lam = { "lam" }

class C {
    val lam = { "lam" }
}

fun foo() {
    ::lam<!SYNTAX!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved<!>)<!>
    ::lam<!SYNTAX!>(::<!DEBUG_INFO_MISSING_UNRESOLVED!>lam<!>)<!>
    ::lam<!SYNTAX!>(fun() {})<!>

    C::lam<!SYNTAX!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved<!>)<!>
    C::lam<!SYNTAX!>(::<!DEBUG_INFO_MISSING_UNRESOLVED!>lam<!>)<!>
    C::lam<!SYNTAX!>(fun() {})<!>

    <!FUNCTION_EXPECTED!>String::class<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    <!FUNCTION_EXPECTED!>String::class<!>(fun() {})
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, lambdaLiteral, propertyDeclaration, stringLiteral */
