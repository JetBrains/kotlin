fun box(): String {
    var result = 0
    val intRange = 1..3
    for (i: Int? in intRange) {
        result = sum(result, i)
    }
    return if (result == 6) "OK" else "fail: $result"
}

fun sum(i: Int, z: Int?): Int {
    return i + z!!
}