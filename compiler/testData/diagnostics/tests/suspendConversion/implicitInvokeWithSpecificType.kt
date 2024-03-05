// ISSUE: KT-62836
fun box() {
    useSuspendFunInt(<!TYPE_MISMATCH!>Test()<!>)
}

fun useSuspendFunInt(fn: suspend () -> String): String = ""

open class Test : () -> String? {
    override fun invoke() = "OK"
}
