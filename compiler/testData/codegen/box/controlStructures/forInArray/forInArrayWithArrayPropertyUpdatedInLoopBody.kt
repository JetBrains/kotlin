// WITH_STDLIB

var xs = intArrayOf(1, 2, 3)

fun box(): String {
    var sum = 0
    for (x in xs) {
        sum = sum * 10 + x
        xs = IntArray(0)
    }
    return if (sum == 123) "OK" else "Fail: $sum"
}