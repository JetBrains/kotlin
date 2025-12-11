// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: Array<String> = Array.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: Array<Any?> = Array.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: Array<Int> = Array.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: Array<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val y: Array<Any?> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[null]<!>
    val z: Array<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
