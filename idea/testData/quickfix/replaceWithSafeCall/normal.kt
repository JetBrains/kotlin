// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a<caret>.length
}