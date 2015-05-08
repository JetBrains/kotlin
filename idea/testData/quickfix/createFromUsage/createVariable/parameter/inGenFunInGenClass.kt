// "Create parameter 'foo'" "true"

class A<T> {
    fun test<T>(n: Int) {
        val t: T = <caret>foo
    }
}
