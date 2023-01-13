// KT-3517 Can't call .equals() on a boolean
// KT-55469
// IGNORE_BACKEND_K2: NATIVE

fun box(): String {
    val a = false
    return if (true.equals(true) && a.equals(false)) "OK" else "fail"
}