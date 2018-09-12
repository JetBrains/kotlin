@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")
@OptionalExpectation
expect annotation class Optional()

@Optional
fun foo() { println(42) }

fun main(args: Array<String>) {
    foo()
}