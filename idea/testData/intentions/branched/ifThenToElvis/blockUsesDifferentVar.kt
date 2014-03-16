// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo = null
    val a = "a"
    val b = "b"

    if (foo != null<caret>) {
        a
    }
    else {
        b
    }

}
