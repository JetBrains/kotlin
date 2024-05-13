class MyClass()

operator fun <T> T.contains(int: Int): Boolean = true

fun usage() {
    when (1) {
        i<caret>n MyClass() -> true
    }
}