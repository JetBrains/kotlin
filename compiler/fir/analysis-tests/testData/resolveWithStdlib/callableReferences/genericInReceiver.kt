fun test_1(a: String, s: String) {
    val pair = s.let(a::to)
}

fun test_2(a: String, s: String) {
    val pair = s.let { a.to(it) }
}