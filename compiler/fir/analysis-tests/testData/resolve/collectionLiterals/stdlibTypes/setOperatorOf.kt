// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: Set<String> = Set.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: Set<Any?> = Set.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: Set<Int> = Set.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: Set<String> = []
    val y: Set<Any?> = [null]
    val z: Set<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
