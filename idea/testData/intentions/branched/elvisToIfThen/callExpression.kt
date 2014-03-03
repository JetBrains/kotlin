fun foo(): String? {
    print("foo")
    return "foo"
}

fun bar() {
    print("bar")
}

fun main(args: Array<String>) {
    foo() ?: <caret>bar()
}
