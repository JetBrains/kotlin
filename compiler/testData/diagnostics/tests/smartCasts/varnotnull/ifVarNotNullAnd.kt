// See KT-5737
fun get(): String? {
    return "abc"
}

fun foo(): Int {
    var ss:String? = get()

    return if (ss != null && <!DEBUG_INFO_SMARTCAST!>ss<!>.length > 0)
        1
    else
        0
}