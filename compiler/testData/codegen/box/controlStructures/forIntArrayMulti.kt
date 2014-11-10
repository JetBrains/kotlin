fun box() : String {
    val a = Array<IntArray> (3, { IntArray(3) })

    for (i in a.indices, j in a[i].indices) {
        a[i][j] = i + j
    }

    var sum = 0
    for (x in a, y in x) {
        sum = sum + y
    }
    if(sum != 18) return "FAIL: $sum"

    return "OK"
}