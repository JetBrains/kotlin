// "Create parameter 'foo'" "true"
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'

enum class E(val foo: Int) {
    A
    B {
        val t: Int = foo
    }
    C
}