// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main(args: Array<String>) {
    val foo: String? = null
    val bar = "bar"

    if (foo != null<caret>) {
        doSomething ("Hello")
        foo
    }
    else {
        bar
    }
}
