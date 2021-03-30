// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// CHECK_BYTECODE_LISTING
// WITH_SIGNATURES

fun <T1, R> call(value: T1, f: (T1) -> R): R {
    return f(value)
}

fun <T2> test(x: T2) = call(x) { it }

fun box() = test("OK")
