// !DIAGNOSTICS: -UNUSED_PARAMETER
fun run(f: () -> Unit) = 0

fun foo(arg: Int?) = run {
    var x = arg
    if (x == null) return@run
    <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
}

class My {
    fun foo(arg: Int?) = run {
        var x = arg
        if (x == null) return@run
        <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    }

    fun Int?.bar() = run {
        var x = this
        if (x == null) return@run
        <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
    }
}