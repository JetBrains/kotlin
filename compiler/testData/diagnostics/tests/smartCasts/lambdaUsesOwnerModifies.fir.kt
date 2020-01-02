// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Not safe: x = null later in the owner
        x.hashCode()
    }
    x = null  
}