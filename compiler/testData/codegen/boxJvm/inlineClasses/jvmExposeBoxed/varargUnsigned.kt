// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

// A user value class has no corresponding array type, so the only vararg *of* a value class is an unsigned
// one: 'vararg counts: UInt' has parameter type 'UIntArray', which is itself a '@JvmInline value class'. The
// declaration is therefore mangled and gets a boxed 'sumOf(UIntArray)' beside it.
//
// There is deliberately no Java caller: javac 8 crashes with a NullPointerException in 'Types.isSubtype' on a
// signature mentioning 'kotlin.UIntArray', so the bytecode listing is the assertion here.
@JvmExposeBoxed
fun sumOf(vararg counts: UInt): UInt {
    var sum = 0u
    for (count in counts) sum += count
    return sum
}

fun box(): String {
    val sum = sumOf(1u, 2u)
    if (sum != 3u) return "FAIL: $sum"
    return "OK"
}
