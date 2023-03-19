// !LANGUAGE: +InlineClasses
// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57211 K2: incorrect "error: an annotation argument must be a compile-time constant" on unsigned array in annotation argument

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
