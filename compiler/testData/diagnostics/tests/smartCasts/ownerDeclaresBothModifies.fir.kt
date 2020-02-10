// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Unsafe because of owner modification
        x.hashCode()
        x = null
    }
    if (x != null) x = 42
    // Unsafe because of lambda
    x.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
}