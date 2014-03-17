// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "abc"
    if (foo != null<caret>) {
        print ("Hello")
        foo.length
    }
    else null
}
