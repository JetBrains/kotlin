fun box(): String {
    val result = for (i in 1..3, j in 1..i) yield for (k in j..i) yield k*i*j
    return if (result.toString() == "[[1], [2, 4], [8], [3, 6, 9], [12, 18], [27]]") "OK" else "FAIL: $result"
}