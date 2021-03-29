// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a.apply {
        this<caret>.length
    }
}
