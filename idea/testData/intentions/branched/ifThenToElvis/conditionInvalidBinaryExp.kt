// IS_APPLICABLE: false
operator fun <T> T.compareTo(a: T): Int = 0

fun main(args: Array<String>) {
    val foo = null
    val bar = "bar"
    if (foo > null<caret>) {
        foo
    }
    else {
        bar
    }
}
