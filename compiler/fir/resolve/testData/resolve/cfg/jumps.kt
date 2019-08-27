fun test_1(x: Int?) {
    val y = if (x == null) {
        throw KotlinNullPointerException()
    } else {
        x
    }
    y.inc()
}

fun test_2(x: Int?) {
    val y = if (x == null) {
        x
    } else {
        x
    }
    y.inc()
}

fun test_3(x: Int?) {
    while (true) {
        x as Int
        break
    }
    x.inc()
}

fun test_4(x: Int?) {
    do {
        x as Int
        break
    } while (true)
    x.inc()
}