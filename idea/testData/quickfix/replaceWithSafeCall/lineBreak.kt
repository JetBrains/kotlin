// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
fun foo(a: String?) {
    val b = a
            .<caret>length
}