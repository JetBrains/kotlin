// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    run {
        var x = arg
        while (x != null) {
            x = <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
            if (x == 0) x = null
        }
    }
}