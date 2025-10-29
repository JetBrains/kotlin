// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

import kotlin.sequences.*

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    val a: Sequence<String> = Sequence.of()
    val b: Sequence<Any?> = Sequence.of(null)
    val c: Sequence<Int> = Sequence.of(1, 2, 3)

    val x: Sequence<String> = []
    val y: Sequence<Any?> = [null]
    val z: Sequence<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, collectionLiteral, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration */
