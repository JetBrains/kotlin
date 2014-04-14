// WITH_RUNTIME
// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (foo == null<caret>)
    else {
        foo
    }
}
