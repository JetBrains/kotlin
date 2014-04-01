fun maybeFoo(): String? {
    return "foo"
}

fun doSomething<T>(a: T) {}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    doSomething(foo)
    if (foo != null<caret>) {
        foo.length
    }
    else {
        null
    }
}
