// WITH_STDLIB
// ISSUE: KT-89919, KT-89920
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4

// MODULE: lib
// FILE: lib.kt

inline fun makeULongFactory(): (ULong, ULong) -> ULongArray = ::ulongArrayOf
inline fun makeUIntFactory(): (UIntArray) -> UIntArray = ::uintArrayOf
fun makeULongFactoryNonInline(): (ULong, ULong) -> ULongArray = ::ulongArrayOf

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val high = 1uL shl 40
    val expected = ulongArrayOf(high, ULong.MAX_VALUE)

    val adapted = makeULongFactory()(high, ULong.MAX_VALUE)
    if (!adapted.contentEquals(expected)) return "Fail inline lib adapted reference"

    val nonAdapted = makeUIntFactory()(uintArrayOf(42u))
    if (!nonAdapted.contentEquals(uintArrayOf(42u))) return "Fail inline lib non-adapted reference"

    val nonInline = makeULongFactoryNonInline()(high, ULong.MAX_VALUE)
    if (!nonInline.contentEquals(expected)) return "Fail non-inline lib reference"

    return "OK"
}
