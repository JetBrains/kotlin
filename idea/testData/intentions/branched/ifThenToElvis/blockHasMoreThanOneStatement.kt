// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = null
    val bar = "bar"

    if (foo != null<caret>) {
        print ("Hello")
        foo
    }
    else {
        bar
    }
}
