
inline fun myRun(b: () -> Unit) = b()

fun foo() {
    <!CAN_BE_VAL_DELAYED_INITIALIZATION!>var<!> <!UNUSED_VARIABLE!>a<!>: Int
    return

    <!UNREACHABLE_CODE!>myRun {
        return
    }<!>
}
