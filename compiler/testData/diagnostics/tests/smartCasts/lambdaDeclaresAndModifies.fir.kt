// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    run {
        var x = arg
        if (x == null) return@run
        x.hashCode()
    }
}