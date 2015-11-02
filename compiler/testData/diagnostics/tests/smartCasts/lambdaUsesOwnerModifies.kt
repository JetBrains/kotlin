// !DIAGNOSTICS: -UNUSED_PARAMETER
fun run(f: () -> Unit) = 0

fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Not safe: x = null later in the owner
        <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
    }
    x = null  
}