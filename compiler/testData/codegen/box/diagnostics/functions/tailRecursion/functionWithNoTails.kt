<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun noTails() {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}
