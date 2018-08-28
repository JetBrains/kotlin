// !USE_EXPERIMENTAL: kotlin.Experimental
// !API_VERSION: 1.3
// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun test() {
    <!EXPERIMENTAL_UNSIGNED_LITERALS!>42u<!>
    <!EXPERIMENTAL_UNSIGNED_LITERALS!>21UL<!>
    val list = listOf(
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>1u<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>0xFFu<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>0xbbU<!>
    )

    takeAll(
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>1u<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>2u<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>3u<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>4u<!>,
        <!EXPERIMENTAL_UNSIGNED_LITERALS!>5u<!>
    )

    @UseExperimental(ExperimentalUnsignedTypes::class) 42u
}

fun takeAll(
    b: <!EXPERIMENTAL_API_USAGE!>UByte<!>,
    s: <!EXPERIMENTAL_API_USAGE!>UShort<!>,
    i: <!EXPERIMENTAL_API_USAGE!>UInt<!>,
    l: <!EXPERIMENTAL_API_USAGE!>ULong<!>,
    vararg uints: <!EXPERIMENTAL_API_USAGE!>UInt<!>
) {}

const val unsignedConst = <!EXPERIMENTAL_UNSIGNED_LITERALS!>0u<!>
const val unsignedLongConst = <!EXPERIMENTAL_UNSIGNED_LITERALS!>0uL<!>
