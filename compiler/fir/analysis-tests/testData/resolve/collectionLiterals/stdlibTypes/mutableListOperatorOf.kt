// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: MutableList<String> = MutableList.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: MutableList<Any?> = MutableList.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: MutableList<Int> = MutableList.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: MutableList<String> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val y: MutableList<Any?> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[null]<!>
    val z: MutableList<Int> = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    x.add("hello")
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
