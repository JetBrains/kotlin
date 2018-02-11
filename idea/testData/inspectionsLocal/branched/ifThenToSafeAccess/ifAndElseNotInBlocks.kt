fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    i<caret>f (foo != null)
        foo.length
    else
        null
}
