// COMPILATION_ERRORS
error class Foo

fun test(x: String | Foo) {
    x|.length
    x|.length|.inc()
}
