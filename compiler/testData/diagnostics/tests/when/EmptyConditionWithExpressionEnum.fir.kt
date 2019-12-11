// EA-68871: empty when condition
enum class My { FIRST, SECOND }
fun foo(arg: My): Int {
    when (arg) {
        My.FIRST -> return 0
        <!SYNTAX!><!>-> return 4
        else -> return -1
    }
}