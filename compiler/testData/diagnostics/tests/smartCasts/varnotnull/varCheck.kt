// RUN_PIPELINE_TILL: BACKEND
fun get(): String? {
    return ""
}

fun foo(): Int {
    var c: String? = get()
    c!!.length
    return <!DEBUG_INFO_SMARTCAST!>c<!>.length // Previous line should make !! unnecessary here.
}