// WITH_RUNTIME
fun foo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    (foo())<caret>!!
}
