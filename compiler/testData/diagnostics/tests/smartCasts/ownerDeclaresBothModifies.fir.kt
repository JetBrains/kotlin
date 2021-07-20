// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Stable because `run` is in-place
        x.hashCode()
        x = null
    }
    if (<!SENSELESS_COMPARISON!>x != null<!>) x = 42
    // Unsafe because of lambda
    x<!UNSAFE_CALL!>.<!>hashCode()
}
