// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun go1(f: () -> String) = f()
inline suspend fun go2(f: () -> String) = f()

fun builder(c: suspend () -> Unit) {}

suspend fun String.id(): String = this

fun box() {
    val x = "f"
    builder {
        <!INAPPLICABLE_CANDIDATE!>go1<!>(<!UNRESOLVED_REFERENCE!>x::id<!>)
        <!INAPPLICABLE_CANDIDATE!>go2<!>(<!UNRESOLVED_REFERENCE!>x::id<!>)
    }
}
