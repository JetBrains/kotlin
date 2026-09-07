
<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec<!> fun test(go: Boolean) : Unit {
    if (!go) return
    try {
        test(false)
    } catch (any : Exception) {
        test(false)
    } finally {
        test(false)
    }
}

fun box(): String {
    test(true)
    return "OK"
}
