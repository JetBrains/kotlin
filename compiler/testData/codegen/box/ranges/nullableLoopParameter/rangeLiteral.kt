// IGNORE_BACKEND: JS_IR
fun box(): String {
    var result = 0
    for (i: Int? in 1..3) {
        result = sum(result, i)
    }
    return if (result == 6) "OK" else "fail: $result"
}

fun sum(i: Int, z: Int?): Int {
    return i + z!!
}