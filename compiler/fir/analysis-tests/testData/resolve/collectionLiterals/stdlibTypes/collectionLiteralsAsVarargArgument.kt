// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@file:OptIn(ExperimentalCollectionLiterals::class, ExperimentalUnsignedTypes::class)

fun takeBytes(vararg bytes: Byte) {}
fun takeShorts(vararg shorts: Short) {}
fun takeInts(vararg ints: Int) {}
fun takeLongs(vararg longs: Long) {}
fun takeFloats(vararg floats: Float) {}
fun takeDoubles(vararg doubles: Double) {}
fun takeBooleans(vararg booleans: Boolean) {}
fun takeChars(vararg chars: Char) {}

fun takeUBytes(vararg ubytes: UByte) {}
fun takeUShorts(vararg ushorts: UShort) {}
fun takeUInts(vararg uints: UInt) {}
fun takeULongs(vararg ulongs: ULong) {}

fun takeStrings(vararg strs: String) {}

fun test() {
    takeBytes(bytes = [])
    takeBytes(*[1, 2, 3])

    takeShorts(shorts = [])
    takeShorts(*[1, 2, 3])

    takeInts(ints = [])
    takeInts(*[1, 2, 3])

    takeLongs(longs = [])
    takeLongs(*[1, 2, 3])

    takeFloats(floats = [])
    takeFloats(*[1.0f, 2.0f, 3.0f])

    takeDoubles(doubles = [])
    takeDoubles(*[1.0, 2.0, 3.0])

    takeBooleans(booleans = [])
    takeBooleans(*[true, false, true])

    takeChars(chars = [])
    takeChars(*['a', 'b', 'c'])

    takeUBytes(ubytes = [])
    takeUBytes(*[1u, 2u, 3u])

    takeUShorts(ushorts = [])
    takeUShorts(*[1u, 2u, 3u])

    takeUInts(uints = [])
    takeUInts(*[1u, 2u, 3u])

    takeULongs(ulongs = [])
    takeULongs(*[1u, 2u, 3u])

    takeStrings(strs = [])
    takeStrings(*["a", "b", "c"])
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral, unsignedLiteral,
vararg */
