const val N = 10

fun sumOdds(): Int {
    var sum = 0
    for (i in 0..N) {
        if (i%2 == 0) continue
        sum += i
    }
    return sum
}

fun box(): String {
    val test = sumOdds()
    if (test != 25) return "Failed: $test"
    return "OK"
}