fun box(): String {
    val result = for ((i, j) in 1..3 zip 2..4, p in i..j) yield p*i*j
    return if (result.toString() == "[2, 4, 12, 18, 36, 48]") "OK" else "FAIL: $result"
}