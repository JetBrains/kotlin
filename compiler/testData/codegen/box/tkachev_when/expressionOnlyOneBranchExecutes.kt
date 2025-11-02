// this may be silly
fun box(): String {
    val x = 1
    var y = 0
    val z = when (x) {
        0 -> {
            y += 1
            0
        }
        1 -> {
            y += 2
            1
        }
        else -> {
            y += 3
            2
        }
    }
    return if (y == 2 && z == 1) "OK" else "fail"
}