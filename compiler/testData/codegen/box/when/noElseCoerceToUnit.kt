var result = "FAIL"
val d = 0.0

fun test(arg: Int) {
    if (arg == 1) {
        result = "firstResult"
        d
    } else if (arg == 2) {
        result = "secondResult"
        arg
    }
}

fun box(): String {
    test(1)
    if (result != "firstResult")
        return "FAIL1"
    test(2)
    if (result != "secondResult")
        return "FAIL2"
    return "OK"
}