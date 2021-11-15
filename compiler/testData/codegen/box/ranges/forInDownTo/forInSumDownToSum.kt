// WITH_STDLIB

fun test(): Int {
    var sum = 0
    for (i in sum downTo sum) {
        sum += 1 + i
    }
    return sum
}

fun box(): String {
    val t1 = test()
    if (t1 != 1) return "Failed: t1=$t1"

    return "OK"
}
