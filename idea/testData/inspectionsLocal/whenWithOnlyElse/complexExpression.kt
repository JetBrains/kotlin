// WITH_RUNTIME

fun println(s: String) {}

fun foo() {
    val a = <caret>when ("") {
        else -> {
            println("")
            1
        }
    }
}