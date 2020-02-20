fun test(
    val f: String.() -> Int = { <!UNRESOLVED_REFERENCE!>length<!> }
): Int {
    return "".f()
}