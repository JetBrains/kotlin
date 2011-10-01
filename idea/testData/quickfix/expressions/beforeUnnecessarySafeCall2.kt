// "Replace to dot call" "true"
fun foo(a: Any) {
    when (a) {
        <caret>?.equals(0) => true
        else => false
    }
}