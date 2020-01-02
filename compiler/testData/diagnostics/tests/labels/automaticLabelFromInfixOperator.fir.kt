fun test(x: List<Int>): Int {
    x myMap {
        return@myMap
    }

    return 0
}

fun myMap(x: List<Int>): Int {
    x myMap {
        return@myMap
    }

    return 0
}

infix fun List<Int>.myMap(x: () -> Unit) {}