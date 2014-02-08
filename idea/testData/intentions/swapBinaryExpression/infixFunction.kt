// IS_APPLICABLE: false
class Foo() {
    fun cat(x: Int): Int {return x}
}

fun main() {
    val catter = Foo() c<caret>at 5
}