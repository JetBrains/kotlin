fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    print(foo)
    if (foo != null<caret>) {
        foo.length()
    }
    else {
        null
    }
}
