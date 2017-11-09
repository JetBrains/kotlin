fun minBiRoot(a: Double, b: Double, c: Double): Double {
    if (a == 0.0) {
        if (b == 0.0) return 1.0
        val bc = -c / b
        if (bc < 0.0) return 2.0
        return -bc
    }
    val d = b * b - 4 * a * c
    if (d < 0.0) return 3.0
    val y1 = (-b + d) / (2 * a)
    val y2 = (-b - d) / (2 * a)
    val y3 = if (y1 > y2) y1 else y2
    return if (y3 < 0.0) 4.0 else -y3
}