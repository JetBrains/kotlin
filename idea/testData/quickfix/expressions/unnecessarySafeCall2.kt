// "Replace with dot call" "true"
fun Any.foo() {
    this<caret>?.equals(0)
}