fun f(p: Int?): Int {
    return when(p) {
        null -> 3
        else -> p!!
    }
}

fun box(): String {
    return if (f(null) == 3) "OK" else "fail"
}
