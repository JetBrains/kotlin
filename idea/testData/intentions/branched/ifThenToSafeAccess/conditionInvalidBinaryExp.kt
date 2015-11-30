// IS_APPLICABLE: false
operator fun <T> T.compareTo(a: T): Int = 0

fun main(args: Array<String>) {
    val foo = "foo"
    if (foo > null<caret>) {
        foo.length
    }
    else null
}
