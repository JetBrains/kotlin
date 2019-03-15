// WITH_RUNTIME
// IS_APPLICABLE: false
fun maybeFoo(): String? {
    return "foo"
}

fun capture(block: () -> Unit): Unit = Unit

fun main(args: Array<String>) {
    var foo = maybeFoo()

    capture {
        foo = null
    }

    if (foo == null<caret>)
        throw NullPointerException()
    else
        foo
}
