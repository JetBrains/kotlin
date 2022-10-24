// This test is needed to check that IrCompileTimeChecker will not fail trying to find and analyze correct toString method

object Obj {
    override fun toString(): String = "OK"

    fun Int.toString(): String = "Not OK"
}

const val a = 1 // this is a dummy const to avoid check in IrInterpreterDumpHandler

fun box(): String {
    return "" + "$Obj" // force a call
}
