// "Create parameter 'foo'" "true"
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'
// ERROR: Missing delegation specifier 'E'

enum class E {
    A
    B {
        val t: Int = <caret>foo
    }
    C
}