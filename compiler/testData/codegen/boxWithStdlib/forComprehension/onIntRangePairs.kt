fun box(): String {
    val result = for ((i, j) in 1..5 zip 2..6) yield i*j
    return if (result.toString() == "[2, 6, 12, 20, 30]") "OK" else "FAIL: $result"
}