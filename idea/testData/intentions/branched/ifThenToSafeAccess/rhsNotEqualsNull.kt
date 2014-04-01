fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    if (null != foo<caret>)
        foo.length
    else
        null
}
