// EA-68871: empty when condition
fun foo(arg: Int): Int {
    when (arg) {
        0 -> return 0
        <!SYNTAX!><!>-> return 4
        else -> return -1
    }
}