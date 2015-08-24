fun simpleWhen() {
    var count = 0
    when (count) {
        1 -> 32
        is Int -> 42
        in 1..3 -> 45
        else -> 44
    }

}

fun complexWhen() {
    var count = 0
    while (count > 0) {
        count++
    }

    when (count) {
        1 -> if (count == 1) count++ else print("123")
        else -> if (count == 1) count++ else print("123")
    }
}

fun whenInsideFor() {
    var count = 0
    while (count > 0) {
        count++
    }

    for (element in 0..4)
        when (element) {
            1 -> if (count == 1) count++ else print("123")
            else -> if (count == 1) count++ else print("123")
        }
}

fun whenInsideWhile() {
    var count = 0
    while (count < 3) {
        count++
    }

    while (count < 10)
        when (count) {
            1 -> if (count == 1) count++ else print("123")
            else -> if (count == 1) count++ else print("123")
        }
}

fun whenWithEnumerationCase(): Int {
    val c = 3
    val d = 4
    val e = 5
    var z = 0
    when(c) {
        5, 4, 3 -> z++;
        else -> {
            z = -1000;
        }
    }

    when(d) {
        5, 4, 3 -> z++;
        else -> {
            z = -1000;
        }
    }

    when(e) {
        5, 4, 3 -> z++;
        else -> {
            z = -1000;
        }
    }
    return z
}

fun whenWithManyEnumerationCase() {
    val c = 3
    val d = 4
    when (c) {
        5, 2 -> 42
        6, 3 -> 43
        7, 4 -> 44
        else -> 45
    }
    when (d) {
        5, 2 -> 42
        6, 3 -> 43
        7, 4 -> 44
        else -> 45
    }
}