
inline fun myRun(b: () -> Unit) = b()

fun foo() {
    <!CAN_BE_VAL!>var<!> <!UNUSED_VARIABLE!>a<!>: Int
    return

    <!UNREACHABLE_CODE!>myRun {
        return
    }<!>
}
