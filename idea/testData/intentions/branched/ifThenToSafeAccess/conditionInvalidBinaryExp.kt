// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo = "foo"
    if (foo > null<caret>) {
        foo.length
    }
    else null
}
