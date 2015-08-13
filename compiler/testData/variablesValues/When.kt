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