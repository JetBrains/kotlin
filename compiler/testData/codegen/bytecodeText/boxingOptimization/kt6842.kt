fun box(): Long {
    val x1 = (1..5).map { it * 40 }
    val x2 = (1..5).fold(0) { x, y -> x + y }
    val x3 = (1..5).reduce { x, y -> x + y }
    val x4 = (1..5).count { it > 0 }

    val y1 = (1L..5L).map { it * 40L }
    val y2 = (1L..5L).fold(0L) { x, y -> x + y }
    val y3 = (1L..5L).reduce { x, y -> x + y }
    val y4 = (1L..5L).count { it > 0L }

    return (x1.first() + x2 + x3 + x4).toLong() + y1.first() + y2 + y3 + y4.toLong()
}

// 5 nextLong
// 5 nextInt
// 0 next\s*\(
