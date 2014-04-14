// WITH_RUNTIME
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (foo != null<caret>) {
    }
    else {
        throw NullPointerException()
    }
}
