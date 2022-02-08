// WITH_STDLIB

fun foo(): Int {
    var sum = 0
    for (c in "239")
        sum += (c.toInt() - '0'.toInt())
    return sum
}

fun box(): String {
    val f = foo()
    return if (f == 14) "OK" else "Fail $f"
}
