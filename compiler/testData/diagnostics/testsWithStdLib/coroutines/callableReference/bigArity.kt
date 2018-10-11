// !LANGUAGE: -FunctionTypesWithBigArity
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

class A {
    suspend fun foo(
        p00: Long = 1, p01: A = A(), p02: A = A(), p03: A = A(), p04: A = A(), p05: A = A(), p06: A = A(), p07: A = A(), p08: A = A(), p09: A = A(),
        p10: A = A(), p11: A = A(), p12: A = A(), p13: A = A(), p14: A = A(), p15: A = A(), p16: A = A(), p17: A = A(), p18: A = A(), p19: A = A(),
        p20: A = A(), p21: A = A(), p22: A = A(), p23: A = A(), p24: A = A(), p25: A = A(), p26: A = A(), p27: A = A(), p28: A = A(), p29: String
    ): String {
        return p29
    }
}

suspend fun expectsLambdaWithBigArity(c: suspend <!UNSUPPORTED_FEATURE!>(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, String) -> String<!>): String {
    return c.invoke(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, "OK")
}
