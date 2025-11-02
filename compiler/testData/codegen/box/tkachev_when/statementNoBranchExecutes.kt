fun box(): String {
    val x = 1
    var y = "OK"
    when (x) {
        0 -> y = "Fail"
        2 -> y = "Fail"
    }
    return y
}