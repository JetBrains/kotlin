<!NO_TAIL_CALLS_FOUND!>tailRecursive fun noTails()<!> {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}