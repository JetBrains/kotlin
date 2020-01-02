// "Add non-null asserted (!!) call" "true"
// WITH_RUNTIME
fun test(s: String?) {
    s.run {
        <caret>length
    }
}