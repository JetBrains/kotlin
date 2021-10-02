// WITH_RUNTIME

fun box(): String {
    return if (run { 123 != intArrayOf() as Any }) "OK" else "Fail"
}