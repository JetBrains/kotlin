// DIAGNOSTICS: -UNUSED_PARAMETER

inline fun go1(f: () -> String) = f()
inline suspend fun go2(f: () -> String) = f()

fun builder(c: suspend () -> Unit) {}

suspend fun String.id(): String = this

fun box() {
    val x = "f"
    builder {
        go1(<!TYPE_MISMATCH!>x::id<!>)
        go2(<!TYPE_MISMATCH!>x::id<!>)
    }
}
