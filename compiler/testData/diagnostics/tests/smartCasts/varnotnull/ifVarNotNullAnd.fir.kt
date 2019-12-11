// See KT-5737
fun get(): String? {
    return "abc"
}

fun foo(): Int {
    var ss:String? = get()

    return if (ss != null && ss.length > 0)
        1
    else
        0
}