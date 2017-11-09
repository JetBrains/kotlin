// "Add non-null asserted (!!) call" "true"
// WITH_RUNTIME
fun String?.foo() {
    <caret>toLowerCase()
}