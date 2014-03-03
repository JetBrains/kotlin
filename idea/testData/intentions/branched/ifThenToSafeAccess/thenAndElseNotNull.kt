// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (foo == null<caret>) {
        foo.length()
    }
    else {
        foo.length()
    }
}
