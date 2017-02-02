// KT-3517 Can't call .equals() on a boolean

fun box(): String {
    val a = false
    return if (true.equals(true) && a.equals(false)) "OK" else "fail"
}