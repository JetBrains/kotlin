// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// FIR_DUMP

// MODULE: companionsOn
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt

fun testOn() {
    val a: MutableList<String> = MutableList.of()
    val b: MutableList<Any?> = MutableList.of(null)
    val c: MutableList<Int> = MutableList.of(1, 2, 3)

    val x: MutableList<String> = []
    val y: MutableList<Any?> = [null]
    val z: MutableList<Int> = [1, 2, 3]

    x.add("hello")
}

// MODULE: companionsOff
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt

fun testOff() {
    val a: MutableList<String> = MutableList.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: MutableList<Any?> = MutableList.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: MutableList<Int> = MutableList.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: MutableList<String> = []
    val y: MutableList<Any?> = [null]
    val z: MutableList<Int> = [1, 2, 3]

    x.add("hello")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
stringLiteral */
