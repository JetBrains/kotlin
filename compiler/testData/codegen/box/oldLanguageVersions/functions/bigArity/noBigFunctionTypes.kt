// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BIG_ARITY
// !LANGUAGE: -FunctionTypesWithBigArity

// This test does not make sense for JVM because a diagnostic is reported when function types with big arity are not available
// (see diagnostics/tests/sourceCompatibility/noBigFunctionTypes.kt)
// IGNORE_BACKEND: JVM, JVM_IR

class A

fun foo(
    p00: A, p01: A, p02: A, p03: A, p04: A, p05: A, p06: A, p07: A, p08: A, p09: A,
    p10: A, p11: A, p12: A, p13: A, p14: A, p15: A, p16: A, p17: A, p18: A, p19: A,
    p20: A, p21: A, p22: A, p23: A, p24: A, p25: A, p26: A, p27: A, p28: A, p29: A
): String = "OK"

fun bar(x: Function30<A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, String>): String {
    val a = A()
    return x(a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a)
}

fun box(): String {
    return bar(::foo)
}
