// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if<caret> {
        foo.length()
    } else null
}
