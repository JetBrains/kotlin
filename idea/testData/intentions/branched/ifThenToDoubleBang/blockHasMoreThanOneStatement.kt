// WITH_RUNTIME
// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = null

    if (foo != null<caret>) {
        print ("Hello")
        foo
    }
    else {
        throw NullPointerException()
    }
}
