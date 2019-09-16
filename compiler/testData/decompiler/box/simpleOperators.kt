fun test1(a: Int, b: Int) = a + b

fun test1x(a: Int, b: Int): Int {
    return a.plus(b)
}

fun test2(a: Int, b: Int) = a - b

fun test2x(a: Int, b: Int): Int {
    return a.minus(b)
}

fun test3(a: Int, b: Int) = a * b

fun test3x(a: Int, b: Int): Int {
    return a.times(b)
}

fun test4(a: Int, b: Int) = a / b

fun test4x(a: Int, b: Int): Int {
    return a.div(b)
}

fun box(): String {
    val t1 = test1(2, 1)
    val t1x = test1x(2, 1)
    val t2 = test2(2, 1)
    val t2x = test2x(2, 1)
    val t3 = test3(2, 1)
    val t3x = test3x(2, 1)
    val t4 = test4(2, 1)
    val t4x = test4x(2, 1)
    when (t1) {
        3 -> return "OK"
        else -> return "FAIL"
    }

}