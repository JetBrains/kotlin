fun foo(b: Boolean): Int {
    val x: Int
    val y: Int
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (b) {
        true -> y = 1
        false -> y = 0
    }<!>
    // x is initialized here
    x = 3
    return x + y
}