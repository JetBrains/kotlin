fun box(): String {
    var r = "OK"
    val x = 3
    val y: Unit = when (x) {
        1 -> { r = "Fail 0" }
        2 -> { r = "Fail 1" }
    }
    return r
}