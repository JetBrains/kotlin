// IGNORE_DIAGNOSTIC_API
// ISSUE: KT-62836
fun box() {
    useSuspendFunInt(Test())
}

fun useSuspendFunInt(fn: suspend () -> String): String = ""

open class Test : () -> String? {
    override fun invoke() = "OK"
}
