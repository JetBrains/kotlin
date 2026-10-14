// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

// MODULE: anno
// FILE: Anno.kt
import kotlin.reflect.KClass

@Repeatable
annotation class Anno(
    val strings: Array<String>,
    val classes: Array<KClass<*>>,
    vararg val names: String,
)

// MODULE: companionsOn(anno)
// LANGUAGE: +CompanionBlocks
// FILE: withCompanions.kt
@Anno(
    Array.of("a"),
    classes = [],
    *Array.of("b", "c"),
)
fun testOn() {
    val a: Array<String> = Array.of()
    val b: Array<Any?> = Array.of(null)
    val c: Array<Int> = Array.of(1, 2, 3)

    val x: Array<String> = []
    val y: Array<Any?> = [null]
    val z: Array<Int> = [1, 2, 3]
}

// MODULE: companionsOff(anno)
// LANGUAGE: -CompanionBlocks
// FILE: withoutCompanions.kt
@Anno(
    ["a"],
    classes = Array.<!UNSUPPORTED_FEATURE!>of<!>(String::class),
    names = [],
)
fun testOff() {
    val a: Array<String> = Array.<!UNSUPPORTED_FEATURE!>of<!>()
    val b: Array<Any?> = Array.<!UNSUPPORTED_FEATURE!>of<!>(null)
    val c: Array<Int> = Array.<!UNSUPPORTED_FEATURE!>of<!>(1, 2, 3)

    val x: Array<String> = []
    val y: Array<Any?> = [null]
    val z: Array<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, functionDeclaration, integerLiteral, localProperty,
nullableType, outProjection, primaryConstructor, propertyDeclaration, starProjection, stringLiteral, vararg */
