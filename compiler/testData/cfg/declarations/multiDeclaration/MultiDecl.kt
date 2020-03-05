package n

class C {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun test(c: C) {
    val (a, b) = c
    val d = 1
}
