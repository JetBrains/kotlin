// WITH_STDLIB

fun box(): String {
    return if (run { 123 != intArrayOf() as Any }) "OK" else "Fail"
}