// "Create function 'foo'" "true"

class A<T>(val n: T) {
    fun test(): A<Int> {
        return this.foo(2, "2")
    }

    private fun foo(i: Int, s: String): A<Int> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}