// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtWhenConditionInRange
class MyClass()

operator fun <T> T.contains(int: Int): Boolean = true

fun usage() {
    when (1) {
        <expr>in MyClass()</expr> -> true
    }
}