fun println(s: String) {}

fun foo() {
    <caret>when ("") {
        else -> {
            println("")
            1
        }
    }
}