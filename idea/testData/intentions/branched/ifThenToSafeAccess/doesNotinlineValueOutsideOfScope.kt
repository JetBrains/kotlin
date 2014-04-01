fun maybeFoo(): String? {
    return "foo"
}

val x = maybeFoo()

fun main(args: Array<String>) {
    if (x !=<caret> null) {
        x.length
    } else {
        null
    }
}
