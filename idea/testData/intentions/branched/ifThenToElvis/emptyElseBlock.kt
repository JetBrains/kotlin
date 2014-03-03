// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = null
    if (foo != null<caret>) {
        foo
    }
    else {

    }
}
