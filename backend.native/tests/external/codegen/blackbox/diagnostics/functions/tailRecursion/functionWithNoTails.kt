// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

<!NO_TAIL_CALLS_FOUND!>tailrec fun noTails()<!> {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}