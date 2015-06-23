package testing

fun bar() {
}

fun bar(a: Int) {
}

fun String.foo() {
}

fun main(args: Array<String>) {
    bar()
    bar(1)
    "".foo()

    bar(2, 3)
}