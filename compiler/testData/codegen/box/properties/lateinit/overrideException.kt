// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

interface Intf {
    val str: String
}

class A : Intf {
    override lateinit var str: String

    fun getMyStr(): String {
        try {
            val a = str
        } catch (e: RuntimeException) {
            return "OK"
        }
        return "FAIL"
    }
}

fun box(): String {
    val a = A()
    return a.getMyStr()
}