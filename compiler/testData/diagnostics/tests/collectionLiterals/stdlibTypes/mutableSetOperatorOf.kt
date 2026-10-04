// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// FIR_DUMP

// MODULE: companionsOn
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt

fun testOn() {
    val a: MutableSet<String> = MutableSet.of()
    val b: MutableSet<Any?> = MutableSet.of(null)
    val c: MutableSet<Int> = MutableSet.of(1, 2, 3)

    val x: MutableSet<String> = []
    val y: MutableSet<Any?> = [null]
    val z: MutableSet<Int> = [1, 2, 3]

    x.add("hello")
}

// MODULE: companionsOff
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt

fun testOff() {
    val a: MutableSet<String> = MutableSet.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: MutableSet<Any?> = MutableSet.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: MutableSet<Int> = MutableSet.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: MutableSet<String> = []
    val y: MutableSet<Any?> = [null]
    val z: MutableSet<Int> = [1, 2, 3]

    x.add("hello")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
stringLiteral */
