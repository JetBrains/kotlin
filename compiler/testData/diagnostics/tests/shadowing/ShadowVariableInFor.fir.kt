// WITH_STDLIB

fun ff(): Int {
    var i = 1
    for (i in 0..10) {
    }

    for ((i, j) in listOf(Pair(1,2))) {
        val i = i
    }

    return i
}
