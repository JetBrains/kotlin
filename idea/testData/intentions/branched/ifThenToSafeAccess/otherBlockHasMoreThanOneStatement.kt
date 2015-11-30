// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(args: Array<String>) {
    val foo: String? = "abc"
    if (foo != null<caret>) {
        foo.length
    }
    else {
        doSomething("Hi")
        null
    }
}
