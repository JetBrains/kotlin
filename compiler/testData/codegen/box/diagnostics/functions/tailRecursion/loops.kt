// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec fun test(x : Int) : Int {
    var z = if (x > 3) 3 else x
    while (z > 0) {
        if (z > 10) {
            return test(x - 1)
        }
        <!NON_TAIL_RECURSIVE_CALL!>test<!>(0)
        z = z - 1
    }

    return 1
}

fun box() : String = if (test(100000) == 1) "OK" else "FAIL"