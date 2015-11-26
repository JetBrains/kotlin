// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(args: Array<String>) {
    val foo: String? = null
    val bar = "bar"

    if (foo != null<caret>) {
        foo
    }
    else {
        doSomething("Hello")
        bar
    }
}
