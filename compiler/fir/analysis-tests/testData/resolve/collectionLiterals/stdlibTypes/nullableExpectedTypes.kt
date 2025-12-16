// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

@file:OptIn(ExperimentalUnsignedTypes::class)

fun test() {
    val a: Array<Int?>? = [1, null, 3]
    val b: Array<Any>? = [1, 2, 3]
    val c: IntArray? = [1, 2, 3]
    val d: LongArray? = [1, 2, 3]
    val e: ByteArray? = [1, 2, 3]
    val f: ShortArray? = [1, 2, 3]
    val g: UIntArray? = [1u, 2u, 3u]
    val h: ULongArray? = []
    val i: UByteArray? = []
    val j: UShortArray? = []
    val k: CharArray? = ['1', '2', '3']

    val l: List<*>? = [{}]
    val m: MutableList<*>? = [::test]
    val n: Set<*>? = [42]
    val o: MutableSet<CharSequence>? = ["42"]
    val p: kotlin.sequences.Sequence<*>? = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration, starProjection, stringLiteral, unsignedLiteral */
