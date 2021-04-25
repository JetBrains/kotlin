// "Add non-null asserted (!!) call" "true"

fun callMe(p: String) {}

fun callIt(p: Any) {
    callMe(<caret>p as String?)
}
