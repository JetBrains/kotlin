// "Replace scope function with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    a.run {
        <caret>length
    }
}