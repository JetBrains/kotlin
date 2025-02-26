// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this option
// WITH_STDLIB

interface I<T> {
    fun foo(p: T)
}

class C : I<Nothing> {
    override fun foo(p: Nothing) {
        println(p.toString())
    }
}

fun box(): String {
    val c = C()
    try {
        val x = (c as I<String>).foo("zzz")
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
