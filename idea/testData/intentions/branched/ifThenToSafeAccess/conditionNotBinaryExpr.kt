// IS_APPLICABLE: false
operator fun String?.times(a: Int): Boolean = a == 0

fun main(args: Array<String>) {
    val foo: String = "foo"
    if (foo * 10<caret>) {
        foo.length
    }
    else null
}
