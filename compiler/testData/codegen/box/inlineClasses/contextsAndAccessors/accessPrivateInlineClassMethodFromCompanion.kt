// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class R(private val r: Int) {
    private fun ok() = "OK"

    companion object {
        fun test(r: R) = r.ok()
    }
}

fun box() = R.test(R(0))