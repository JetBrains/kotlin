// IS_APPLICABLE: false
fun main(args: Array<String>) {
    var foo: String? = "foo"
    var bar: String? = "bar"
    if (foo != null<caret>) {
        bar?.length
    }
    else null
}
