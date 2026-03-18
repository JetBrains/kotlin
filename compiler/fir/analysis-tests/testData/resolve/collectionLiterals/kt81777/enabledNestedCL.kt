// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76150
// LANGUAGE: +CollectionLiterals

fun test() {
    <!UNRESOLVED_REFERENCE!>[<!UNRESOLVED_REFERENCE!>[]<!>]<!>
    <!UNRESOLVED_REFERENCE!>[<!UNRESOLVED_REFERENCE!>["2"]<!>]<!>
    <!UNRESOLVED_REFERENCE!>[{}, <!UNRESOLVED_REFERENCE!>[]<!>]<!>
    <!UNRESOLVED_REFERENCE!>[::test, <!UNRESOLVED_REFERENCE!>[2]<!>]<!>
    <!UNRESOLVED_REFERENCE!>[42, <!UNRESOLVED_REFERENCE!>[]<!>]<!>
}

/* GENERATED_FIR_TAGS: callableReference, collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral,
stringLiteral */
