// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// This test is needed to check that IrCompileTimeChecker will not fail trying to find and analyze correct toString method

object Obj {
    override fun toString(): String = "OK"

    fun Int.toString(): String = "Not OK"
}

fun box(): String {
    return "" + "$Obj" // force a call
}
