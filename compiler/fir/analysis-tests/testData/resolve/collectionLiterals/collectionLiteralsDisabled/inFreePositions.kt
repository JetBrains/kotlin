// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

fun test() {
    <!UNSUPPORTED!>[]<!>
    <!UNSUPPORTED!>[1, 2, 3]<!>
    <!UNSUPPORTED!>["1", "2", "3"]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, stringLiteral */
