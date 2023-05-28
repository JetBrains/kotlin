// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// This test is needed to check that IrCompileTimeChecker will not fail trying to find and analyze correct toString method

object Obj {
    override fun toString(): String = "OK"

    fun Int.toString(): String = "Not OK"
}

fun box(): String {
    return "" + "$Obj" // force a call
}
