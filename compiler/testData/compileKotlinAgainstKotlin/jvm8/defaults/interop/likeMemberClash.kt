// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// FILE: 1.kt
// !JVM_DEFAULT_MODE: disable
interface Check {
    fun test(): String {
        return "fail";
    }
}

interface SubCheck : Check {
    override fun test(): String {
        return "OK"
    }
}


open class CheckClass : Check

// FILE: main.kt
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
class SubCheckClass : CheckClass(), SubCheck

fun box(): String {
    return SubCheckClass().test()
}
