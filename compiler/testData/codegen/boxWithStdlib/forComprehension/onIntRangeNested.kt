fun box(): String {
    val result = for (i in 1..3) yield for (j in 1..i) yield i*j
    return if (result.toString() == "[[1], [2, 4], [3, 6, 9]]") "OK" else "FAIL: $result"
}