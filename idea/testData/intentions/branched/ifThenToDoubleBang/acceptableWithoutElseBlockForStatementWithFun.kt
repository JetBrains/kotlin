// WITH_RUNTIME
fun foo(): Any? = "foo"

fun main(args: Array<String>) {
    if (foo() == null<caret>) {
        throw KotlinNullPointerException()
    }
}
