interface IC1 {
    operator fun component1(): Int
}

interface IC2 {
    operator fun component2(): String
}

fun test(x: Any) {
    if (x is IC1 && x is IC2) {
        val (x1, x2) = x
    }
}
