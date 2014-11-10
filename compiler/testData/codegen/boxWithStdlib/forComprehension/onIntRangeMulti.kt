fun box(): String {
    val result = for (i in 1..3, j in i..i*2) yield i + j
    return if (result.toString() == "[2, 3, 4, 5, 6, 6, 7, 8, 9]") "OK" else "FAIL: $result"
}