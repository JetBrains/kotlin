// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun test() {
    val a: MutableSet<String> = MutableSet.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: MutableSet<Any?> = MutableSet.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: MutableSet<Int> = MutableSet.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: MutableSet<String> = []
    val y: MutableSet<Any?> = [null]
    val z: MutableSet<Int> = [1, 2, 3]

    x.add("hello")
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
