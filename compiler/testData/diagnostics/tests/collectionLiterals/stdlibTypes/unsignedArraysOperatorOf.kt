// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

// MODULE: anno
// FILE: Anno.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

@Repeatable
annotation class Anno(
    val int: UIntArray,
    val byte: UByteArray,
    val long: ULongArray,
    vararg val short: UShort,
)

// MODULE: companionsOn(anno)
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

@Anno(
    UIntArray.of(),
    byte = [],
    long = [42u],
    *UShortArray.of(1u, 2u, 3u),
)
fun testOn() {
    val a0: UByteArray = UByteArray.of()
    val aM: UByteArray = UByteArray.of(1u, 2u)
    val b0: UShortArray = UShortArray.of()
    val bM: UShortArray = UShortArray.of(1u, 2u)
    val c0: UIntArray = UIntArray.of()
    val cM: UIntArray = UIntArray.of(1u, 2u)
    val d0: ULongArray = ULongArray.of()
    val dM: ULongArray = ULongArray.of(1uL, 2uL)

    val x0: UByteArray = []
    val xM: UByteArray = [1u, 2u]
    val y0: UShortArray = []
    val yM: UShortArray = [1u, 2u]
    val z0: UIntArray = []
    val zM: UIntArray = [1u, 2u]
    val w0: ULongArray = []
    val wM: ULongArray = [1uL, 2uL]
}

// MODULE: companionsOff(anno)
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

@Anno(
    [1u, 2u, 3u],
    byte = UByteArray.<!UNSUPPORTED_FEATURE!>of<!>(42u),
    long = ULongArray.<!UNSUPPORTED_FEATURE!>of<!>(),
    short = [],
)
fun testOff() {
    val a0: UByteArray = UByteArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val aM: UByteArray = UByteArray.<!UNSUPPORTED_FEATURE!>of<!>(1u, 2u)
    val b0: UShortArray = UShortArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val bM: UShortArray = UShortArray.<!UNSUPPORTED_FEATURE!>of<!>(1u, 2u)
    val c0: UIntArray = UIntArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val cM: UIntArray = UIntArray.<!UNSUPPORTED_FEATURE!>of<!>(1u, 2u)
    val d0: ULongArray = ULongArray.<!UNSUPPORTED_FEATURE!>of<!>()
    val dM: ULongArray = ULongArray.<!UNSUPPORTED_FEATURE!>of<!>(1uL, 2uL)

    val x0: UByteArray = []
    val xM: UByteArray = [1u, 2u]
    val y0: UShortArray = []
    val yM: UShortArray = [1u, 2u]
    val z0: UIntArray = []
    val zM: UIntArray = [1u, 2u]
    val w0: ULongArray = []
    val wM: ULongArray = [1uL, 2uL]
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, localProperty,
propertyDeclaration, unsignedLiteral */
