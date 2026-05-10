// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: Array<String> = Array.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: Array<Any?> = Array.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: Array<Int> = Array.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: Array<String> = []
    val y: Array<Any?> = [null]
    val z: Array<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
