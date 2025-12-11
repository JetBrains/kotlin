// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: List<String> = List.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: List<Any?> = List.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: List<Int> = List.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: List<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val y: List<Any?> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[null]<!>
    val z: List<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
