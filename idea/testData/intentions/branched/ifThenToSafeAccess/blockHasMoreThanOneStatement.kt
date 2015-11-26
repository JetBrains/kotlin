// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(args: Array<String>) {
    val foo: String? = "abc"
    if (foo != null<caret>) {
        doSomething ("Hello")
        foo.length
    }
    else null
}
