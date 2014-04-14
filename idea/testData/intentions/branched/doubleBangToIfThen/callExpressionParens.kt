// WITH_RUNTIME
fun foo(): String? {
    print("foo")
    return "foo"
}

fun main(args: Array<String>) {
    (foo())<caret>!!
}
