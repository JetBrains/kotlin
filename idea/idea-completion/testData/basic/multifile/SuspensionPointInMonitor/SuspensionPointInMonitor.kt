// FIR_COMPARISON
inline val C.v: Int get() = 1
class C {
    fun f(block: suspend C.() -> Unit): Unit = TODO()
}
suspend fun yield() = {}
fun f(c: C) {
    c.f {
        v.toString()
        yie<caret>
    }
}

// EXIST: yield
