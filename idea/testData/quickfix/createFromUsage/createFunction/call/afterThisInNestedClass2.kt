// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    inner class B<U>(val m: U) {
        fun test(): A<Int> {
            return this@A.foo(2, "2")
        }
    }

    fun foo(i: Int, s: String): A<Int> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}