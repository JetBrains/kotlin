// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

lateinit var result1: String
lateinit var result2: String

class Test(val x: String) {
    fun test(a: String) {
        if (result1 != a) throw AssertionError("result1: $result1")
        result2 = a
    }

    init {
        fun test() {
            fun test1() {
                result1 = x
            }
            test1()
        }
        test()
    }
}

fun box(): String {
    val t = Test("OK")
    t.test("OK")

    return result2
}