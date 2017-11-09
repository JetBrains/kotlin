// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec fun sum(x: Long, sum: Long): Long {
    if (x == 0.toLong()) return sum
    return sum(x - 1, sum + x)
}

fun box() : String {
    val sum = sum(1000000, 0)
    if (sum != 500000500000.toLong()) return "Fail $sum"
    return "OK"
}