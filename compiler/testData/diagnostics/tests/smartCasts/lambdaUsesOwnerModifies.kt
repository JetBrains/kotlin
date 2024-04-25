// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Safe: since `run` is in-place
        <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
    }
    x = null  
}