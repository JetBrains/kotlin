const val N = 10

fun sumUntil6(): Int {
    var sum = 0
    for (i in 0..N) {
        if (i == 6) break
        sum += i
    }
    return sum
}

fun box(): String {
    val test = sumUntil6()
    if (test != 15) return "Failed: $test"
    return "OK"
}