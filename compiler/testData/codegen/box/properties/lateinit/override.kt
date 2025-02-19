// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

interface Intf {
    val str: String
}

class A : Intf {
    override lateinit var str: String

    fun setMyStr() {
        str = "OK"
    }

    fun getMyStr(): String {
        return str
    }
}

fun box(): String {
    val a = A()
    a.setMyStr()
    return a.getMyStr()
}