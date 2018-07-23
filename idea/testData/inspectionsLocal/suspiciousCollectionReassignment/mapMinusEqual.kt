// WITH_RUNTIME
fun test() {
    var map = mapOf(1 to 2)
    <caret>map -= 1 to 2
}