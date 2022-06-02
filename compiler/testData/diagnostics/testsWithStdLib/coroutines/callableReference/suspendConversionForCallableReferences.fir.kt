// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun go1(f: () -> String) = f()
inline suspend fun go2(f: () -> String) = f()

fun builder(c: suspend () -> Unit) {}

suspend fun String.id(): String = this

fun box() {
    val x = "f"
    builder {
        <!INAPPLICABLE_CANDIDATE!>go1<!>(x::<!UNRESOLVED_REFERENCE!>id<!>)
        <!INAPPLICABLE_CANDIDATE!>go2<!>(x::<!UNRESOLVED_REFERENCE!>id<!>)
    }
}
