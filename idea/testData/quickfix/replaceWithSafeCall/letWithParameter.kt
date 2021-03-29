// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a.let { b ->
        b<caret>.length
    }
}
