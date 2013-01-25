fun box(): String {
    val x = 2
    return when(x) {
        in (1..3) -> "OK"
        else -> "fail"
    }
}
