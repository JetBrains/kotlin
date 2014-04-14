// WITH_RUNTIME
class NullPointerException : Exception()

fun foo(): Any? = "foo"

fun main(args: Array<String>) {
    if (foo() == null<caret>) {
        throw java.lang.NullPointerException()
    }
}
