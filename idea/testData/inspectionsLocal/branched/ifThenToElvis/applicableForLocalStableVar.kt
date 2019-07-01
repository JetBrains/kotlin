// WITH_RUNTIME
fun maybeFoo(): String? {
    return "foo"
}

fun test(): String? {
    var foo = maybeFoo()
    val bar = if (foo == null<caret>)
        "hello"
    else
        foo
    return foo
}
