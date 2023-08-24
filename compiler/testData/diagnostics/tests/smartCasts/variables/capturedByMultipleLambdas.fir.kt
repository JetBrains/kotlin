import kotlin.contracts.*

@Suppress(<!ERROR_SUPPRESSION!>"OPT_IN_USAGE_ERROR"<!>, "OPT_IN_USAGE_FUTURE_ERROR")
fun foo(f1: () -> Unit, f2: () -> Unit) {
    contract {
        callsInPlace(f1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(f2, InvocationKind.EXACTLY_ONCE)
    }
    f2()
    f1()
}

fun test() {
    var s: String? = null
    s = ""
    foo(
        { <!SMARTCAST_IMPOSSIBLE!>s<!>.length }, // unstable since lambda evaluation order is indeterministic
        { s = null },
    )
    s = ""
    foo(
        { s = null },
        { <!SMARTCAST_IMPOSSIBLE!>s<!>.length }, // unstable since lambda evaluation order is indeterministic
    )
    s = ""
    foo(
        { s.length }, // stable
        { s.length }, // stable
    )
    s = null
}
