// FULL_JDK
// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: 1.kt
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

// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
// FILE: main.kt
class SubCheckClass : CheckClass(), SubCheck

fun box(): String {
    return SubCheckClass().test()
}
