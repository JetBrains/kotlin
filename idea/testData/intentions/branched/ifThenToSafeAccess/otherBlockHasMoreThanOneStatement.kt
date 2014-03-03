// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "abc"
    if (foo != null<caret>) {
        foo.length
    }
    else {
        print("Hi")
        null
    }
}
