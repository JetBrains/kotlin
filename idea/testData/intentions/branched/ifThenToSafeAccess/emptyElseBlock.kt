// IS_APPLICABLE: false

fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    if (foo != null<caret>) {
        foo.length
    }
    else {

    }
}
