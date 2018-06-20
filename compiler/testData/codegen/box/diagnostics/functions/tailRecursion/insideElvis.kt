// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

tailrec fun test(counter : Int) : Int? {
    if (counter < 0) return null
    if (counter == 0) return 777

    return <!NON_TAIL_RECURSIVE_CALL!>test<!>(-1) ?: <!NON_TAIL_RECURSIVE_CALL!>test<!>(-2) ?: test(counter - 1)
}

fun box() : String =
    if (test(100000) == 777) "OK"
    else "FAIL"