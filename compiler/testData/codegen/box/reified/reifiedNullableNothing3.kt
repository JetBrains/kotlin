// LANGUAGE: +NullableNothingInReifiedPosition
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this language feature
// ISSUE: KT-54227
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: JVM_IR

fun box(): String {
    val arr = arrayOf(null, null)
    if (arr[0] != null) return "FAIL 0"
    if (arr[1] != null) return "FAIL 1"
    if (arr.size != 2) return "FAIL 2"
    return "OK"
}
