class A

fun test(a: Any) {
    var q: String? = null

    when (a) {
        is A -> q = "1"
    }
    // When is not exhaustive
    return <!TYPE_MISMATCH!>q<!>
}
