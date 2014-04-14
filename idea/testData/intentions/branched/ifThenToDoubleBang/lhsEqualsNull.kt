// WITH_RUNTIME
fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    if (foo == null<caret>)
        throw NullPointerException()
    else
        foo
}
