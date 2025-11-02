fun box(): String {
    var a = 1
    var result = "OK"
    when (a) {
        1 -> a = 2
        2 -> result = "fail"
    }
    return result
}