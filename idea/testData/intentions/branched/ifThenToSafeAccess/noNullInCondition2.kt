// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    val bar: String? = null
    if (foo == bar<caret>) {
        bar?.length
    }
    else null
}
