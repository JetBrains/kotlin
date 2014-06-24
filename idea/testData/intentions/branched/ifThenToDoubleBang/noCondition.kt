// WITH_RUNTIME
// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if<caret> {
        foo
    } else throw NullPointerException()
}
