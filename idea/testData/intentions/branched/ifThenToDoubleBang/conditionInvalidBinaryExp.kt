// WITH_RUNTIME
// IS_APPLICABLE: false

operator fun <T> T.compareTo(a: T): Int = 0

fun main(args: Array<String>) {
    val foo = null
    if (foo > null<caret>) {
        foo
    }
    else {
        throw NullPointerException()
    }
}
