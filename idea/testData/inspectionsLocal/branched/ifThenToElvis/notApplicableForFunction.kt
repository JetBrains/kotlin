// PROBLEM: none
fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    if (maybeFoo() == null<caret>)
        maybeFoo()
    else
        "bar"
}
