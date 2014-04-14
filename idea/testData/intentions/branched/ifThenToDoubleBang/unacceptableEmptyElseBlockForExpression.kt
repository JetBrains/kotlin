// IS_APPLICABLE: false
// WITH_RUNTIME
fun main(args: Array<String>) {
    val foo: String? = "foo"
    val y = if (foo == null<caret>) {
        throw NullPointerException()
    }
    else {
    }
}
