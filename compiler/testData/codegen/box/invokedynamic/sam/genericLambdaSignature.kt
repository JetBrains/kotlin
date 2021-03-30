// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// CHECK_BYTECODE_LISTING
// WITH_SIGNATURES

fun interface FunIFace<T0, R> {
    fun call(arg: T0): R
}

fun <T1, R> call(value: T1, f: FunIFace<T1, R>): R {
    return f.call(value)
}

fun <T2> test(x: T2) = call(x) { it }

fun box() = test("OK")
