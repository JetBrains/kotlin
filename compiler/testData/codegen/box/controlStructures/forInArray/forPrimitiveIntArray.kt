// WITH_RUNTIME

fun box(): String {
    val a = IntArray(5)
    var sum = 0
    for (i in 0..4) {
        a[i] = i + 1
    }
    for (el in a) {
        sum = sum + el
    }
    if (sum != 15) return "failed: sum=$sum"

    return "OK"
}
