fun box(): String {
    val x = 1
    val y = when (x) {
        0, 2, 4 -> y += 1
        1, 3, 5 -> y += 2
        else -> y += 3
    }
    return if (y == 2) "OK" else "fail"
}