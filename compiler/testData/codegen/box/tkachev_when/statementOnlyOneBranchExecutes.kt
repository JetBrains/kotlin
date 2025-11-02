fun box(): String {
    val x = 1
    var y = 0
    when (x) {
        0 -> y += 1
        1 -> y += 2
        else -> y += 3
    }
    return if (y == 2) "OK" else "fail"
}