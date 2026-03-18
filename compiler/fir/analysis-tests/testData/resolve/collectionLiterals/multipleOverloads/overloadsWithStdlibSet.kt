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
    foo([42])
    foo(["42"])
    <!NONE_APPLICABLE!>foo<!>([42u])
    <!NONE_APPLICABLE!>foo<!>([42L])
    <!NONE_APPLICABLE!>foo<!>([42, "42"])
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, stringLiteral, unsignedLiteral */
