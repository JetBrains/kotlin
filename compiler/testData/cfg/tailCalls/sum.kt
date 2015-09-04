tailrec fun sum(x: Long, sum: Long): Long {
    if (x == 0.toLong()) return sum
    return sum(x - 1, sum + x)
}