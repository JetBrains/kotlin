<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec<!> fun noTails() {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}
