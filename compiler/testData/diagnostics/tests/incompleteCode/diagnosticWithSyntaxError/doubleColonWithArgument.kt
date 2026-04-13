// RUN_PIPELINE_TILL: FRONTEND
val lam = { "lam" }

class C {
    val lam = { "lam" }
}

fun foo() {
    ::lam<!SYNTAX!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)<!>
    ::lam<!SYNTAX!>(::lam)<!>
    ::lam<!SYNTAX!>(fun() {})<!>

    C::lam<!SYNTAX!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)<!>
    C::lam<!SYNTAX!>(::lam)<!>
    C::lam<!SYNTAX!>(fun() {})<!>

    <!UNRESOLVED_REFERENCE("invoke")!>String::class<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    <!UNRESOLVED_REFERENCE("invoke")!>String::class<!>(fun() {})
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, lambdaLiteral, propertyDeclaration, stringLiteral */
