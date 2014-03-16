// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String = "foo"
    if (foo * 10<caret>) {
        foo.length
    }
    else null
}
