// "Create parameter 'foo'" "true"
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'

enum class E(foo: Int) {
    A
    B
    C

    val t: Int = foo
}