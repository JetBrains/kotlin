// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// FIR_DUMP

// MODULE: companionsOn
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt

fun testOn() {
    val a: Set<String> = Set.of()
    val b: Set<Any?> = Set.of(null)
    val c: Set<Int> = Set.of(1, 2, 3)

    val x: Set<String> = []
    val y: Set<Any?> = [null]
    val z: Set<Int> = [1, 2, 3]
}

// MODULE: companionsOff
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt

fun testOff() {
    val a: Set<String> = Set.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: Set<Any?> = Set.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: Set<Int> = Set.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: Set<String> = []
    val y: Set<Any?> = [null]
    val z: Set<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration */
