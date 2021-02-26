// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a.let {
        it<caret>.length
    }
}
/* FIR_COMPARISON */