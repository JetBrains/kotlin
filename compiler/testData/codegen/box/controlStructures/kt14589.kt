// WITH_RUNTIME

fun box(): String {
    var array = arrayOf(1, 2, 3)
    var sum = 0
    for (x in array) {
        sum += x
        array = emptyArray()
    }

    return if (sum == 6) "OK" else "Fail"
}
