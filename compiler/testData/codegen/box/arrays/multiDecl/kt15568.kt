fun box(): String {
    val a = Array(2) { DoubleArray(3) }

    for (i in 1..1) {
        for (j in 0..2) {
            a[i][j] += a[i - 1][j]
        }
    }

    if (a[0][0] != 0.0) return "fail ${a[0][0]}"

    return "OK"
}