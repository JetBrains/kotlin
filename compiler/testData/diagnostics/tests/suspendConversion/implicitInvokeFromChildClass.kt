// FIR_IDENTICAL
// ISSUE: KT-62836
fun box() {
    useSuspendFunInt(Child())
}

fun useSuspendFunInt(fn: suspend () -> String): String = ""

abstract class Test : () -> String

open class Child: Test() {
    override fun invoke() = "OK"
}