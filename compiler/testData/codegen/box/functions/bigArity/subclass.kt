// !LANGUAGE: +FunctionTypesWithBigArity

// Implementing function interface is prohibited in JavaScript
// IGNORE_BACKEND: JS_IR, JS

class A(val value: String)

class Fun : (A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A) -> String {
    override fun invoke(
        p1: A, p2: A, p3: A, p4: A, p5: A, p6: A, p7: A, p8: A, p9: A,
        p10: A, p11: A, p12: A, p13: A, p14: A, p15: A, p16: A, p17: A, p18: A, p19: A,
        p20: A, p21: A, p22: A, p23: A, p24: A, p25: A, p26: A, p27: A, p28: A, p29: A,
        p30: A, p31: A, p32: A, p33: A, p34: A, p35: A, p36: A, p37: A, p38: A, p39: A,
        p40: A, p41: A, p42: A
    ): String {
        return p21.value + p32.value
    }
}

fun box(): String {
    val a = A("")
    val f = Fun()
    return f(a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, A("O"), a, a, a, a, a, a, a, a, a, a, A("K"), a, a, a, a, a, a, a, a, a, a)
}
