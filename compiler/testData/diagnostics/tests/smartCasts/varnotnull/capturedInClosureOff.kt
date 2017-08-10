// !LANGUAGE: -CapturedInClosureSmartCasts

fun run(f: () -> Unit) = f()

fun foo(s: String?) {
    var x: String? = null
    if (s != null) {
        x = s
    }
    if (x != null) {
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
    }
}
