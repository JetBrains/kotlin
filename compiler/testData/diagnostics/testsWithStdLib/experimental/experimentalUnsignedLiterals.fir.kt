// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun test() {
    42u
    21UL
    val list = listOf(
        1u,
        0xFFu,
        0xbbU
    )

    <!INAPPLICABLE_CANDIDATE!>takeAll<!>(
        1u,
        2u,
        3u,
        4u,
        5u
    )

    @OptIn(ExperimentalUnsignedTypes::class) 42u
}

fun takeAll(
    b: UByte,
    s: UShort,
    i: UInt,
    l: ULong,
    vararg uints: UInt
) {}

const val unsignedConst = 0u
const val unsignedLongConst = 0uL
