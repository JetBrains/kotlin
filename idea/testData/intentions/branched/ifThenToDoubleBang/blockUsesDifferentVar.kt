// WITH_RUNTIME
// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo = null
    val a = "a"

    if (foo != null<caret>) {
        a
    }
    else {
        throw NullPointerException()
    }

}
