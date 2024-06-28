// FIR_IDENTICAL
fun interface Runnable {
    fun run()
}

fun foo(r: Runnable, f: Runnable) = 1
fun foo(r: Runnable, f: () -> Unit) = ""

fun test(): String {
    return foo(Runnable {}, {})
}