fun box(): String {
    val result = for (i in 1..5) yield i*i
    return if (result.toString() == "[1, 4, 9, 16, 25]") "OK" else "FAIL: $result"
}