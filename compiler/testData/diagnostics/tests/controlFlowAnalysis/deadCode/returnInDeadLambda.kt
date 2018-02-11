
inline fun myRun(b: () -> Unit) = b()

fun foo() {
    var <!UNUSED_VARIABLE!>a<!>: Int
    return

    <!UNREACHABLE_CODE!>myRun {
        return
    }<!>
}
