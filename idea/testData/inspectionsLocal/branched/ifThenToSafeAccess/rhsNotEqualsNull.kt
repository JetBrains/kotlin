fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    <caret>if (null != foo)
        foo.length
    else
        null
}
