// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtWhenConditionInRange
class MyClass() {
    operator fun contains(str: String): Boolean = false
}

operator fun <T> T.contains(int: Int): Boolean = true

fun usage() {
    when (val f = 3) {
        <expr>!in MyClass()</expr> -> false
    }
}