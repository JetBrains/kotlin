// !DIAGNOSTICS: -UNUSED_PARAMETER
fun run(f: () -> Unit) = 0

fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Unsafe because of owner modification
        <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        x = null
    }
    if (x != null) x = 42
    // Unsafe because of lambda
    <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
}