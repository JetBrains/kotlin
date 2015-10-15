// See KT-5737
fun get(): Any {
    return "abc"
}

fun foo(): Int {
    var ss: Any = get()

    return if (ss is String && <!DEBUG_INFO_SMARTCAST!>ss<!>.length > 0)
        1
    else
        0
}