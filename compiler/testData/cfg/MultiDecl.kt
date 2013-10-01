package n

class C {
    fun component1() = 1
    fun component2() = 2
}

fun test(c: C) {
    val (a, b) = c
    val d = 1
}
