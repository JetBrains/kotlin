// WITH_RUNTIME
//IS_APPLICABLE: false
fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    var foo = maybeFoo()
    if (foo == null<caret>)
        throw NullPointerException()
    else
        foo
}
