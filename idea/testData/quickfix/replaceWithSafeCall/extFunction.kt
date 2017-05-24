// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun String?.foo() {
    <caret>toLowerCase()
}