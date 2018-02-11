// WITH_RUNTIME
fun test() {
    <caret>when {
        false -> {
            println(1)
        }
    }
}