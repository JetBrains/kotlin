// "Replace with safe (this?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a.apply {
        <caret>length
    }
}
/* FIR_COMPARISON */