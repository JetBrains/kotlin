// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

class A {
    public lateinit var str: String
}

fun box(): String {
    val a = A()
    try {
        a.str
    } catch (e: RuntimeException) {
        return "OK"
    }
    return "FAIL"
}