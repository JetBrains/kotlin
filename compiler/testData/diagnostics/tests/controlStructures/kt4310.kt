package f

fun test(a: Boolean, b: Boolean): Int {
    return if(a) {
        1
    } else {
        <!TYPE_MISMATCH!>if (b) {
            3
        }<!>
    }    // no error, but must be
}