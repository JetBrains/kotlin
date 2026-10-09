// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// FIR_DUMP

// MODULE: companionsOn
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt

fun testOn() {
    val a: List<String> = List.of()
    val b: List<Any?> = List.of(null)
    val c: List<Int> = List.of(1, 2, 3)

    val x: List<String> = []
    val y: List<Any?> = [null]
    val z: List<Int> = [1, 2, 3]
}

// MODULE: companionsOff
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt

fun testOff() {
    val a: List<String> = List.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: List<Any?> = List.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: List<Int> = List.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: List<String> = []
    val y: List<Any?> = [null]
    val z: List<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration */
