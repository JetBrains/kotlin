// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// FIR_DUMP

// MODULE: companionsOn
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt

fun testOn() {
    val a: Sequence<String> = Sequence.of()
    val b: Sequence<Any?> = Sequence.of(null)
    val c: Sequence<Int> = Sequence.of(1, 2, 3)

    val x: Sequence<String> = []
    val y: Sequence<Any?> = [null]
    val z: Sequence<Int> = [1, 2, 3]
}

// MODULE: companionsOff
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt

fun testOff() {
    val a: Sequence<String> = Sequence.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: Sequence<Any?> = Sequence.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: Sequence<Int> = Sequence.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: Sequence<String> = []
    val y: Sequence<Any?> = [null]
    val z: Sequence<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, collectionLiteral, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration */
