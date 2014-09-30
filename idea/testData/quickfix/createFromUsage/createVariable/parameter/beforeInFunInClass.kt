// "Create parameter 'foo'" "true"

class A {
    fun test(n: Int) {
        val t: Int = <caret>foo
    }
}
