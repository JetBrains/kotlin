// "Create parameter 'foo'" "true"

class A {
    fun test<T>(n: Int) {
        val t: T = <caret>foo
    }
}
