// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_KT_DUMP

// MODULE: lib
// FILE: lib.kt
annotation class Anno(
    val b: UByte = 255u,
    val s: UShort = 65535u,
    val i: UInt = 4294967295u,
    val l: ULong = 18446744073709551615u,
)

// MODULE: main(lib)
// DUMP_EXTERNAL_CLASS: Anno
// FILE: main.kt
@Anno
fun test() {}
