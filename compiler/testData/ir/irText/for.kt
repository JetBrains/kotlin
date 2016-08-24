fun testIterable(ss: List<String>) {
    for (s in ss) {
        println(s)
    }
}

fun testDestructuring(pp: List<Pair<Int, String>>) {
    for ((i, s) in pp) {
        println(i)
        println(s)
    }
}

