// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: Int? = 4
    val bar = 3
    if (foo * 10<caret>) {
        foo
    }
    else {
        bar
    }
}
