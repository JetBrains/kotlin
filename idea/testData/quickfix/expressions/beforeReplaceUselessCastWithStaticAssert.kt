// "Replace a cast with a static assert" "true"
fun foo(a: String) {
    val b = a <caret>as Any
}
