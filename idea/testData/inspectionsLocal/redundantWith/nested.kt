// WITH_RUNTIME
fun test() {
    <caret>with ("") {
        with ("a") {
            this
        }
    }
}