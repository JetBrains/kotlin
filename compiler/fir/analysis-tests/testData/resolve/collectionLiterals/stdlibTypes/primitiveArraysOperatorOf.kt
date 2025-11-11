// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@file:OptIn(ExperimentalCollectionLiterals::class, ExperimentalUnsignedTypes::class)

fun <T> take(vararg t: T) {
}

fun test() {
    take<IntArray>([], [42], [1, 2, 3])
    take<LongArray>([], [42], [1, 2, 3])
    take<ByteArray>([], [42], [1, 2, 3])
    take<ShortArray>([], [42], [1, 2, 3])
    take<BooleanArray>([], [false], [true, false, true])
    take<CharArray>([], ['*'], ['a', 'b', 'c'])
    take<DoubleArray>([], [42.0], [1.0, 2.0, 3.0])
    take<FloatArray>([], [42f], [1f, 2f, 3f])

    take<UIntArray>([], [42u], [1u, 2u, 3u])
    take<ULongArray>([], [42uL], [1uL, 2uL, 3uL])
    take<UShortArray>([], [42.toUShort()], [1.toUShort(), 2.toUShort(), 3.toUShort()])
    take<UByteArray>([], [42.toUByte()], [1.toUByte(), 2.toUByte(), 3.toUByte()])
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral, nullableType,
typeParameter, unsignedLiteral, vararg */
