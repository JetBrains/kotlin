// "Create parameter 'foo'" "true"

class A {
    fun test(n: Int) {
        <caret>foo = n + 1
    }
}
