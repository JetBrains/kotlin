// "Replace with safe (this?.) call" "true"
// WITH_RUNTIME
fun String?.foo() {
    <caret>toLowerCase()
}
/* IGNORE_FIR */