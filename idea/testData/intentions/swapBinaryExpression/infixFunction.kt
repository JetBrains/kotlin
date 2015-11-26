// IS_APPLICABLE: false
class Foo() {
    infix fun cat(x: Int): Int {return x}
}

fun main() {
    val catter = Foo() c<caret>at 5
}