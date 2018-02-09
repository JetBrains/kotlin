
fun test(x: Int, y: Int): String {
    var result: String
    if (x == 6) {
        if (y == 6) {
            result = "a"
        } else {
            result = "b"
        }
    } else {
        result = "c"
    }
    return result
}

fun box(): String {
    if (test(9, 10) != "c")
        return "Failures"
    return "OK"
}