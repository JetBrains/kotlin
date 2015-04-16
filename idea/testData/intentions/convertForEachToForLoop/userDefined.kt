// IS_APPLICABLE: false
// WITH_RUNTIME
class Foo {
    fun forEach(predicate: (Int) -> Int): Int = predicate(0)
}

fun main() {
    val x = Foo()

    <caret>x.forEach({ it * 2 })
}