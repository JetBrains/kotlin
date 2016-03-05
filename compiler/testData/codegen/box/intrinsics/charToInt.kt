fun box(): String {
    val x: Any = 'A'
    var y = 0
    if (x is Char) {
        y = x.toInt()
    }
    return if (y == 65) "OK" else "fail"
}
