// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun foo(a: Set<Int>) {
}

fun foo(b: Set<String>) {
}

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>([42])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(["42"])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>([42u])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>([42L])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>([42, "42"])
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, stringLiteral, unsignedLiteral */
