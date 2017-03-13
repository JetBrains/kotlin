fun box(): String {
    val i: Int = 10000
    if (!(i === i)) return "Fail int ==="
    if (i !== i) return "Fail int !=="

    val j: Long = 123L
    if (!(j === j)) return "Fail long ==="
    if (j !== j) return "Fail long !=="

    val d: Double = 3.14
    if (!(d === d)) return "Fail double ==="
    if (d !== d) return "Fail double !=="

    return "OK"
}
