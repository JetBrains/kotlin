// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo = null
    if (foo == null<caret>) {
        null
    }
    else {
        null
    }
}
