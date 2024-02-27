// ISSUE: KT-63525

fun box(): String {
    var b: String? = "abc"
    b = null
    var x = 0
    if (b != null) {
        x += b.length
    }
    return when (x) {
        0 -> "OK"
        else -> "Fail: $x"
    }
}
