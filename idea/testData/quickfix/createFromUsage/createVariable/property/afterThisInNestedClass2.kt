// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    private val foo: A<Int>

    inner class B<U>(val m: U) {
        fun test(): A<Int> {
            return this@A.foo
        }
    }
}
