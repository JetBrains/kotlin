fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    val x = if (foo == null<caret>) {
        null
    }
    else {
        foo.length
    }
}
