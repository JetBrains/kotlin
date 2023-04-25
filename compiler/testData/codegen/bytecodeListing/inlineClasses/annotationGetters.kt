// !LANGUAGE: +InlineClasses
// WITH_STDLIB

annotation class Ann(
    val u: UInt,
    val uba: UByteArray,
    val usa: UShortArray,
    val uia: UIntArray,
    val ula: ULongArray
)

@Ann(
    1u,
    [1u],
    ushortArrayOf(),
    [1u, 1u],
    ulongArrayOf(1u, 1u)
)
fun foo() {}
