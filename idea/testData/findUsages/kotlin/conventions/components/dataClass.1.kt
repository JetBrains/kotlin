import pack.A

fun test() {
    for ((x, y, z) in arrayOf<A>()) {
    }

    for (a in listOf<A>()) {
        val (x, y) = a
    }
}
