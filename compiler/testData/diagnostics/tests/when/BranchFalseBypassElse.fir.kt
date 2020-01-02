class A

fun test(a: Any): String {
    var q: String?

    when (a) {
        is A -> q = "1"
        else -> q = "2"
    }
    // When is not exhaustive
    return q
}
