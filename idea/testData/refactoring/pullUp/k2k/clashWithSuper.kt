open class A<T, U> {
    private fun foo(t: String, u: U) {

    }
}

class <caret>B<X>: A<String, X>() {
    // INFO: {"checked": "true"}
    fun foo(s: String, x: X) {

    }
}