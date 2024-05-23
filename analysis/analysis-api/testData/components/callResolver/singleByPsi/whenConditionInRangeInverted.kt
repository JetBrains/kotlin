class MyClass() {
    operator fun contains(str: String): Boolean = false
}

operator fun <T> T.contains(int: Int): Boolean = true

fun usage() {
    when (val f = 3) {
        <expr>!in MyClass()</expr> -> false
    }
}