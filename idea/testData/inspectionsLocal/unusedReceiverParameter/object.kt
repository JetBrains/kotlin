object Test

fun <caret>Test.foo() = 42

fun main(args: Array<String>) {
    val x = Test
    x.foo()
}