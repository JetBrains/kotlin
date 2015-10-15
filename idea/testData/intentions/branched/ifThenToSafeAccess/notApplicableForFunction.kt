//IS_APPLICABLE: false
fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    if (maybeFoo() == null<caret>)
        null
    else
        maybeFoo()?.length
}
