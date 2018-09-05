// !LANGUAGE: +FunctionTypesWithBigArity
// IGNORE_BACKEND: JVM_IR

class A(val value: Int)

private fun check(actual: A, expected: Int) {
    if (expected != actual.value) {
        throw AssertionError("Expected $expected, actual ${actual.value}")
    }
}

fun box(): String {
    val l = {
        p00: A, p01: A, p02: A, p03: A, p04: A, p05: A, p06: A, p07: A, p08: A, p09: A,
        p10: A, p11: A, p12: A, p13: A, p14: A, p15: A, p16: A, p17: A, p18: A, p19: A,
        p20: A, p21: A, p22: A, p23: A, p24: A, p25: A, p26: A, p27: A, p28: A, p29: A ->
        check(p00, 0)
        check(p01, 1)
        check(p02, 2)
        check(p03, 3)
        check(p04, 4)
        check(p05, 5)
        check(p06, 6)
        check(p07, 7)
        check(p08, 8)
        check(p09, 9)
        check(p10, 10)
        check(p11, 11)
        check(p12, 12)
        check(p13, 13)
        check(p14, 14)
        check(p15, 15)
        check(p16, 16)
        check(p17, 17)
        check(p18, 18)
        check(p19, 19)
        check(p20, 20)
        check(p21, 21)
        check(p22, 22)
        check(p23, 23)
        check(p24, 24)
        check(p25, 25)
        check(p26, 26)
        check(p27, 27)
        check(p28, 28)
        check(p29, 29)
        "OK"
    }

    return l(
        A(0), A(1), A(2), A(3), A(4), A(5), A(6), A(7), A(8), A(9),
        A(10), A(11), A(12), A(13), A(14), A(15), A(16), A(17), A(18), A(19),
        A(20), A(21), A(22), A(23), A(24), A(25), A(26), A(27), A(28), A(29)
    )
}
