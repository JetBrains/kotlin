// ISSUE: KT-54227
// IGNORE_BACKEND_K1: ANY

fun box(): String {
    val arr = arrayOf(null, null)
    if (arr[0] != null) return "FAIL 0"
    if (arr[1] != null) return "FAIL 1"
    if (arr.size != 2) return "FAIL 2"
    return "OK"
}
