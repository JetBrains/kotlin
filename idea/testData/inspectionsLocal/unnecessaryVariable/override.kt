// PROBLEM: none
abstract class Foo {
    abstract val bar: Int
    abstract fun baz()
}

fun println(i: Int) {}

fun test() {
    val i = 1
    object : Foo() {
        override val <caret>bar = i
        override fun baz() {
            println(bar)
        }
    }
}