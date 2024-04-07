// FIR_IDENTICAL
fun box() = useSuspendFunInt(Test())

fun useSuspendFunInt(fn: suspend () -> String): String = ""

open class Test : () -> String {
    override fun invoke() = "OK"
}
