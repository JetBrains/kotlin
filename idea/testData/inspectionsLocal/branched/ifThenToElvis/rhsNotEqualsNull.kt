fun maybeFoo(): String? {
    return "foo"
}

fun bar(): String = "bar"

fun main(args: Array<String>) {
    val foo = maybeFoo()
    if (null != foo<caret>)
        foo
    else
        bar()
}
