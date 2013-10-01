fun box(): String {
    var r = "Fail 0"
    val x = 1
    val y: Unit = when (x) {
        1 -> { r = "OK" }
        2 -> { r = "Fail 1" }
    }
    return r
}