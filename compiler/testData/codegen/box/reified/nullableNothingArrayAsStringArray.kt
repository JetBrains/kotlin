// LANGUAGE: +NullableNothingInReifiedPosition
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-71528

fun useStringArray(arr: Array<out String?>): String? {
    return arr[0]
}

fun box(): String {
    val arr: Array<Nothing?> = arrayOf<Nothing?>(null, null)
    if (useStringArray(arr) != null) return "FAIL: ${useStringArray(arr)}"
    return "OK"
}
