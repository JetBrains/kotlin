// WITH_RUNTIME
// IS_APPLICABLE: false
fun maybeFoo(): String? {
    return "foo"
}

fun capture(block: () -> Unit): Unit = Unit

fun test(): String? {
    var foo = maybeFoo()

    capture {
        foo = null
    }

    val bar = if (foo == null<caret>)
        42
    else
        foo

    return foo
}
