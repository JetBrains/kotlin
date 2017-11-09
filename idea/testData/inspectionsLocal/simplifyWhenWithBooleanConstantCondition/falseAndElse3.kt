// WITH_RUNTIME
fun test() {
    <caret>when {
        false -> {
            println(1)
        }
        else -> {
            println(2)
        }
    }
}