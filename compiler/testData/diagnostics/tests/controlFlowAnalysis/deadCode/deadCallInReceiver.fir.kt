fun test11() {
    fun Any.bar(i: Int) {}
    todo().bar(1)
}

fun test12() {
    fun Any.bar(i: Int) {}
    todo()?.bar(1)
}

fun todo(): Nothing = throw Exception()