// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

<!NO_TAIL_CALLS_FOUND!>tailrec fun noTails()<!> {
    // nothing here
}

fun box(): String {
    noTails()
    return "OK"
}