fun <T> doSomething(a: T) {}

fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    doSomething(foo)
    val bar = "bar"
    if (foo != null<caret>) {
        foo
    }
    else {
        bar
    }
}
