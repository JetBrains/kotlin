// WITH_RUNTIME
// IS_APPLICABLE: FALSE
fun main(args: Array<String>) {
    val foo: String? = "foo"
    val y = if (foo == null<caret>) {
    }
    else {
        throw NullPointerException()
    }
}
