// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

import kotlin.sequences.*

fun test() {
    val a: Sequence<String> = Sequence.<!UNRESOLVED_REFERENCE!>of<!>()
    val b: Sequence<Any?> = Sequence.<!UNRESOLVED_REFERENCE!>of<!>(null)
    val c: Sequence<Int> = Sequence.<!UNRESOLVED_REFERENCE!>of<!>(1, 2, 3)

    val x: Sequence<String> = []
    val y: Sequence<Any?> = [null]
    val z: Sequence<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, collectionLiteral, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration */
