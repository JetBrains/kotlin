// "Create parameter 'foo'" "true"

class A {
    fun <T> test(n: Int) {
        val t: T = <caret>foo
    }
}
